package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.AgendaTurnoDTO;
import com.mobdata.pontocerto.dto.AlocacaoRequestDTO;
import com.mobdata.pontocerto.dto.AlocacaoResponseDTO;
import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.service.AlocacaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class AlocacaoController {

    @Autowired
    private AlocacaoService alocacaoService;

    @PostMapping("/alocacoes")
    public ResponseEntity<Alocacao> alocar(@Valid @RequestBody AlocacaoRequestDTO request) {
        Alocacao alocacao = alocacaoService.alocar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(alocacao);
    }

    @PatchMapping("/alocacoes/{id}/encerrar")
    public ResponseEntity<Alocacao> encerrar(@PathVariable UUID id, @RequestParam LocalDate dataFim) {
        return ResponseEntity.ok(alocacaoService.encerrar(id, dataFim));
    }

    @GetMapping("/usuarios/{id}/alocacoes")
    public ResponseEntity<List<AlocacaoResponseDTO>> historicoDoFuncionario(@PathVariable UUID id) {
        return ResponseEntity.ok(alocacaoService.historicoDoFuncionario(id));
    }

    @GetMapping("/setores/{id}/alocacoes-atuais")
    public ResponseEntity<List<AlocacaoResponseDTO>> alocadosAtualmenteNoSetor(@PathVariable UUID id) {
        return ResponseEntity.ok(alocacaoService.alocadosAtualmenteNoSetor(id));
    }

    @GetMapping("/usuarios/{id}/agenda")
    public ResponseEntity<List<AgendaTurnoDTO>> agenda(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(alocacaoService.agenda(id, dataInicio, dataFim));
    }
}