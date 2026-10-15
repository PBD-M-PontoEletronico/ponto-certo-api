package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.AberturaResponseDTO;
import com.mobdata.pontocerto.dto.AlocacaoResponseDTO;
import com.mobdata.pontocerto.dto.PreferenciaUsuarioDTO;
import com.mobdata.pontocerto.security.TenantContext;
import com.mobdata.pontocerto.service.AberturaService;
import com.mobdata.pontocerto.service.AlocacaoService;
import com.mobdata.pontocerto.service.PreferenciaUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MeController {

    @Autowired
    private AlocacaoService alocacaoService;

    @Autowired
    private PreferenciaUsuarioService preferenciaUsuarioService;

    @Autowired
    private AberturaService aberturaService;

    @GetMapping("/me/alocacoes")
    public ResponseEntity<List<AlocacaoResponseDTO>> minhasAlocacoes() {
        List<AlocacaoResponseDTO> alocacoes = alocacaoService.historicoDoFuncionario(TenantContext.getUsuarioId());
        return ResponseEntity.ok(alocacoes);
    }

    /**
     * W08 — chamado pela tela de abertura do app. Devolve de uma vez tudo o
     * que o aparelho guarda para bater ponto sem rede, mais a hora do servidor.
     * Ex.: GET /me/abertura?dias=14  (dias de agenda à frente; padrão 14, máx. 45)
     */
    @GetMapping("/me/abertura")
    public ResponseEntity<AberturaResponseDTO> abertura(@RequestParam(required = false) Integer dias) {
        AberturaResponseDTO abertura = aberturaService.montar(TenantContext.getUsuarioId(), dias);
        // Sem cache: uma resposta guardada traria uma hora do servidor velha
        // e o desvio do relógio calculado no app sairia errado.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(abertura);
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
