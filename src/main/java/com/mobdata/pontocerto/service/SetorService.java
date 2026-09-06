package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.SetorRequestDTO;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.repository.EmpresaRepository;
import com.mobdata.pontocerto.repository.SetorRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SetorService {

    @Autowired
    private SetorRepository setorRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Setor cadastrar(SetorRequestDTO request) {
        Empresa empresa = resolverEmpresa(request.empresaId());

        Setor setor = new Setor();
        setor.setNome(request.nome());
        setor.setEndereco(request.endereco());
        setor.setLatitude(request.latitude());
        setor.setLongitude(request.longitude());
        setor.setRaioMetros(request.raioMetros());
        setor.setExigirSelfie(request.exigirSelfie());
        setor.setPoliticaForaPerimetro(request.politicaForaPerimetro());
        setor.setIgnorarLocalizacao(request.ignorarLocalizacao());
        setor.setEmpresa(empresa);

        return setorRepository.save(setor);
    }

    public List<Setor> listar(UUID empresaIdParam) {
        UUID empresaId = TenantContext.isSuperAdmin() ? empresaIdParam : TenantContext.getEmpresaId();

        if (empresaId == null) {
            throw new IllegalArgumentException("Informe a empresa (empresaId) para listar os setores");
        }

        return setorRepository.findAllByEmpresaId(empresaId);
    }

    public Setor buscarPorId(UUID id) {
        Setor setor = setorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Setor não encontrado"));

        verificarAcesso(setor);
        return setor;
    }

    public void excluir(UUID id) {
        Setor setor = buscarPorId(id);

        if (usuarioRepository.existsBySetorId(setor.getId())) {
            throw new IllegalStateException(
                    "Não é possível excluir o setor \"" + setor.getNome()
                            + "\": há funcionário(s) alocado(s) nele. Realoque-os antes de excluir."
            );
        }

        setorRepository.delete(setor);
    }

    private Empresa resolverEmpresa(UUID empresaIdDoRequest) {
        UUID empresaId = TenantContext.isSuperAdmin()
                ? empresaIdDoRequest
                : TenantContext.getEmpresaId();

        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa é obrigatória para cadastrar um setor");
        }

        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
    }

    private void verificarAcesso(Setor setor) {
        if (TenantContext.isSuperAdmin()) {
            return;
        }

        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null || !setor.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalArgumentException("Setor não encontrado");
        }
    }
}