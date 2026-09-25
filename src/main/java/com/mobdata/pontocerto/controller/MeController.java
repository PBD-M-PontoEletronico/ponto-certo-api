package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.AlocacaoResponseDTO;
import com.mobdata.pontocerto.dto.PreferenciaUsuarioDTO;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.security.TenantContext;
import com.mobdata.pontocerto.service.AlocacaoService;
import com.mobdata.pontocerto.service.PreferenciaUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MeController {

    @Autowired
    private AlocacaoService alocacaoService;

    @Autowired
    private PreferenciaUsuarioService preferenciaUsuarioService;

    @GetMapping("/me/alocacoes")
    public ResponseEntity<List<AlocacaoResponseDTO>> minhasAlocacoes() {
        List<AlocacaoResponseDTO> alocacoes = alocacaoService.historicoDoFuncionario(TenantContext.getUsuarioId());
        return ResponseEntity.ok(alocacoes);
    }

    @GetMapping("/me/preferencias")
    public ResponseEntity<PreferenciaUsuarioDTO> minhasPreferencias() {
        var tema = preferenciaUsuarioService.obterTema(TenantContext.getUsuarioId());
        return ResponseEntity.ok(new PreferenciaUsuarioDTO(tema));
    }

    @PatchMapping("/me/preferencias")
    public ResponseEntity<PreferenciaUsuarioDTO> atualizarPreferencias(@RequestBody PreferenciaUsuarioDTO request) {
        var tema = preferenciaUsuarioService.atualizarTema(TenantContext.getUsuarioId(), request.tema());
        return ResponseEntity.ok(new PreferenciaUsuarioDTO(tema));
    }
}
