package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.AlocacaoResponseDTO;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.security.TenantContext;
import com.mobdata.pontocerto.service.AlocacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MeController {

    @Autowired
    private AlocacaoService alocacaoService;


    @GetMapping("/me/alocacoes")
    public ResponseEntity<List<AlocacaoResponseDTO>> minhasAlocacoes() {
        List<AlocacaoResponseDTO> alocacoes = alocacaoService.historicoDoFuncionario(TenantContext.getUsuarioId());
        return ResponseEntity.ok(alocacoes);
    }
}
