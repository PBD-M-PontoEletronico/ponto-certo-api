package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.FeriadoRequestDTO;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.model.Feriado;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.repository.EmpresaRepository;
import com.mobdata.pontocerto.repository.FeriadoRepository;
import com.mobdata.pontocerto.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class FeriadoService {

    @Autowired
    private FeriadoRepository feriadoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private SetorService setorService; // reaproveita a checagem de acesso ao setor

    public Feriado cadastrar(FeriadoRequestDTO request) {
        Empresa empresa = resolverEmpresa(request.empresaId());

        Setor setor = null;
        if (request.setorId() != null) {
            setor = setorService.buscarPorId(request.setorId()); // já valida empresa
            if (!setor.getEmpresa().getId().equals(empresa.getId())) {
                throw new IllegalArgumentException("O setor não pertence à empresa informada");
            }
        }

        boolean jaExiste = setor == null
                ? feriadoRepository.existsByEmpresaIdAndDataAndSetorIsNull(empresa.getId(), request.data())
                : feriadoRepository.existsByEmpresaIdAndDataAndSetorId(empresa.getId(), request.data(), setor.getId());

        if (jaExiste) {
            throw new IllegalStateException("Já existe um feriado nessa data para "
                    + (setor == null ? "a empresa inteira" : "o setor \"" + setor.getNome() + "\""));
        }

        Feriado feriado = new Feriado();
        feriado.setEmpresa(empresa);
        feriado.setSetor(setor);
        feriado.setData(request.data());
        feriado.setDescricao(request.descricao().trim());

        return feriadoRepository.save(feriado);
    }

    public List<Feriado> listar(UUID empresaIdParam, Integer ano) {
        UUID empresaId = TenantContext.isSuperAdmin() ? empresaIdParam : TenantContext.getEmpresaId();

        if (empresaId == null) {
            throw new IllegalArgumentException("Informe a empresa (empresaId) para listar os feriados");
        }

        if (ano == null) {
            return feriadoRepository.findAllByEmpresaIdOrderByDataAsc(empresaId);
        }

        return feriadoRepository.findAllByEmpresaIdAndDataBetweenOrderByDataAsc(
                empresaId, LocalDate.of(ano, 1, 1), LocalDate.of(ano, 12, 31));
    }

    public void excluir(UUID id) {
        Feriado feriado = feriadoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feriado não encontrado"));

        verificarAcesso(feriado);
        feriadoRepository.delete(feriado);
    }

    private Empresa resolverEmpresa(UUID empresaIdDoRequest) {
        UUID empresaId = TenantContext.isSuperAdmin()
                ? empresaIdDoRequest
                : TenantContext.getEmpresaId();

        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa é obrigatória para cadastrar um feriado");
        }

        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
    }

    private void verificarAcesso(Feriado feriado) {
        if (TenantContext.isSuperAdmin()) {
            return;
        }

        UUID empresaId = TenantContext.getEmpresaId();
        if (empresaId == null || !feriado.getEmpresa().getId().equals(empresaId)) {
            throw new IllegalArgumentException("Feriado não encontrado");
        }
    }
}
