package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AfastamentoForm;
import com.mobdata.pontocerto.model.Afastamento;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.AfastamentoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class AfastamentoService {

    private static final DateTimeFormatter FORMATO_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private AfastamentoRepository afastamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AnexoStorageService anexoStorageService;

    @Transactional
    public Afastamento cadastrar(AfastamentoForm form) {
        Usuario usuario = buscarUsuarioComAcesso(form.getUsuarioId());

        LocalDate inicio = form.getDataInicio();
        LocalDate fim = form.getDataFim();

        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Data de fim não pode ser anterior à data de início");
        }

        validarSemSobreposicao(usuario, inicio, fim);

        Afastamento afastamento = new Afastamento();
        afastamento.setUsuario(usuario);
        afastamento.setTipo(form.getTipo());
        afastamento.setDataInicio(inicio);
        afastamento.setDataFim(fim);

        AnexoStorageService.AnexoSalvo anexoSalvo = null;
        MultipartFile anexo = form.getAnexo();
        if (anexo != null && !anexo.isEmpty()) {
            anexoSalvo = anexoStorageService.salvar(anexo);
            afastamento.setAnexoArquivo(anexoSalvo.arquivo());
            afastamento.setAnexoNomeOriginal(anexoSalvo.nomeOriginal());
            afastamento.setAnexoTipoConteudo(anexoSalvo.tipoConteudo());
        }

        try {
            return afastamentoRepository.save(afastamento);
        } catch (RuntimeException e) {
            if (anexoSalvo != null) {
                anexoStorageService.remover(anexoSalvo.arquivo()); // não deixa arquivo órfão
            }
            throw e;
        }
    }

    public List<Afastamento> listar(UUID empresaIdParam, UUID usuarioId) {
        if (usuarioId != null) {
            buscarUsuarioComAcesso(usuarioId);
            return afastamentoRepository.findAllByUsuarioIdOrderByDataInicioDesc(usuarioId);
        }

        UUID empresaId = TenantContext.isSuperAdmin() ? empresaIdParam : TenantContext.getEmpresaId();

        if (empresaId == null) {
            throw new IllegalArgumentException("Informe a empresa (empresaId) para listar os afastamentos");
        }

        return afastamentoRepository.findAllByUsuarioEmpresaIdOrderByDataInicioDesc(empresaId);
    }

    @Transactional
    public void excluir(UUID id) {
        Afastamento afastamento = buscarComAcesso(id);
        String arquivo = afastamento.getAnexoArquivo();

        afastamentoRepository.delete(afastamento);

        if (arquivo != null) {
            anexoStorageService.remover(arquivo);
        }
    }

    public AnexoDownload baixarAnexo(UUID id) {
        Afastamento afastamento = buscarComAcesso(id);

        if (afastamento.getAnexoArquivo() == null) {
            throw new IllegalArgumentException("Este afastamento não tem anexo");
        }

        return new AnexoDownload(
                afastamento.getAnexoNomeOriginal(),
                afastamento.getAnexoTipoConteudo(),
                anexoStorageService.ler(afastamento.getAnexoArquivo())
        );
    }

    /**
     * Um funcionário não pode ter dois afastamentos cobrindo o mesmo dia.
     * Encostar (um termina num dia e o outro começa no dia seguinte) é aceito;
     * dividir o mesmo dia entre dois afastamentos não.
     */
    private void validarSemSobreposicao(Usuario usuario, LocalDate inicio, LocalDate fim) {
        List<Afastamento> sobrepostos = afastamentoRepository.findSobrepostos(usuario.getId(), inicio, fim);

        if (!sobrepostos.isEmpty()) {
            Afastamento existente = sobrepostos.get(0);
            throw new IllegalStateException(
                    "Afastamento recusado: \"" + usuario.getNome() + "\" já tem "
                            + existente.getTipo().getRotulo().toLowerCase()
                            + " de " + existente.getDataInicio().format(FORMATO_BR)
                            + " a " + existente.getDataFim().format(FORMATO_BR)
                            + ", que se sobrepõe ao período informado"
            );
        }
    }

    private Afastamento buscarComAcesso(UUID id) {
        Afastamento afastamento = afastamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Afastamento não encontrado"));

        verificarAcessoAoUsuario(afastamento.getUsuario(), "Afastamento não encontrado");
        return afastamento;
    }

    private Usuario buscarUsuarioComAcesso(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        verificarAcessoAoUsuario(usuario, "Funcionário não encontrado");
        return usuario;
    }

    private void verificarAcessoAoUsuario(Usuario usuario, String mensagem) {
        if (TenantContext.isSuperAdmin()) {
            return;
        }

        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null || usuario.getEmpresa() == null || !usuario.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    public record AnexoDownload(String nome, String tipoConteudo, byte[] conteudo) {
    }
}
