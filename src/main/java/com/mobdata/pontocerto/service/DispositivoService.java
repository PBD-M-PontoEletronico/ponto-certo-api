package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.DispositivoFiltroDTO;
import com.mobdata.pontocerto.dto.DispositivoResponseDTO;
import com.mobdata.pontocerto.dto.PaginaDTO;
import com.mobdata.pontocerto.model.Dispositivo;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.TipoDispositivo;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.DispositivoRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import com.mobdata.pontocerto.specification.DispositivoSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DispositivoService {

    @Autowired
    private DispositivoRepository dispositivoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Chamado a cada login bem-sucedido. Se o aparelho é o mesmo de sempre,
    // só atualiza o "último acesso"; se é novo, desativa o anterior e
    // registra este como o dispositivo ativo do usuário.
    public void vincular(UUID usuarioId, String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        var ativoAtual = dispositivoRepository.findByUsuarioIdAndAtivoTrue(usuarioId);

        if (ativoAtual.isPresent() && ativoAtual.get().getIdentificador().equals(identificador)) {
            ativoAtual.get().setUltimoAcesso(agora);
            dispositivoRepository.save(ativoAtual.get());
            return;
        }

        ativoAtual.ifPresent(anterior -> {
            anterior.setAtivo(false);
            dispositivoRepository.save(anterior);
        });

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        Dispositivo novo = new Dispositivo();
        novo.setTipo(usuario.getPerfil() == Perfil.USUARIO_SETOR
                ? TipoDispositivo.RELOGIO_SETOR
                : TipoDispositivo.PESSOAL);
        novo.setIdentificador(identificador);
        novo.setUsuario(usuario);
        novo.setAtivo(true);
        novo.setDataVinculo(agora);
        novo.setUltimoAcesso(agora);
        dispositivoRepository.save(novo);
    }

    public PaginaDTO<DispositivoResponseDTO> buscar(DispositivoFiltroDTO filtro, Pageable pageable) {
        // Superadmin pode deixar a empresa em branco pra ver de todas ao mesmo
        // tempo; RH_ADMIN sempre vê só a própria (empresaId nunca é null aqui).
        UUID empresaId = TenantContext.isSuperAdmin() ? filtro.empresaId() : TenantContext.getEmpresaId();

        Specification<Dispositivo> spec = Specification
                .where(DispositivoSpecification.comUsuarioNome(filtro.usuarioNome()))
                .and(DispositivoSpecification.comTipo(filtro.tipo()))
                .and(DispositivoSpecification.comSetorId(filtro.setorId()))
                .and(DispositivoSpecification.comAtivo(filtro.ativo()))
                .and(DispositivoSpecification.comEmpresaId(empresaId))
                .and(DispositivoSpecification.comUltimoAcessoEntre(filtro.ultimoAcessoDe(), filtro.ultimoAcessoAte()))
                .and(DispositivoSpecification.comDataVinculoEntre(filtro.dataVinculoDe(), filtro.dataVinculoAte()));

        Page<DispositivoResponseDTO> pagina = dispositivoRepository.findAll(spec, pageable)
                .map(DispositivoResponseDTO::fromEntity);

        return PaginaDTO.fromPage(pagina);
    }

    public void revogar(UUID dispositivoId) {
        Dispositivo dispositivo = dispositivoRepository.findById(dispositivoId)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo não encontrado"));

        verificarAcesso(dispositivo);

        dispositivo.setAtivo(false);
        dispositivoRepository.save(dispositivo);
    }

    private void verificarAcesso(Dispositivo dispositivo) {
        if (TenantContext.isSuperAdmin()) {
            return;
        }

        UUID empresaId = TenantContext.getEmpresaId();
        UUID empresaDoDispositivo = dispositivo.getUsuario().getEmpresa() != null
                ? dispositivo.getUsuario().getEmpresa().getId()
                : null;

        if (empresaId == null || !empresaId.equals(empresaDoDispositivo)) {
            throw new IllegalArgumentException("Dispositivo não encontrado");
        }
    }
}