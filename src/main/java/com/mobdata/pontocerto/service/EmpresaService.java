package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.EmpresaRequestDTO;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmpresaService {

    @Autowired
    private EmpresaRepository empresaRepository;

    public Empresa cadastrar(EmpresaRequestDTO request) {
        Empresa empresa = new Empresa();
        empresa.setRazaoSocial(request.razaoSocial());
        empresa.setContato(request.contato());
        empresa.setAtiva(true); // toda empresa nasce ativa

        return empresaRepository.save(empresa);
    }

    public List<Empresa> listar() {
        return empresaRepository.findAll();
    }

    public Empresa buscarPorId(UUID id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
    }

    public Empresa atualizarSituacao(UUID id, boolean ativa) {
        Empresa empresa = buscarPorId(id);
        empresa.setAtiva(ativa);
        return empresaRepository.save(empresa);
    }
}
