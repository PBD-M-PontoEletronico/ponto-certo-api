package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.AgendaTurnoDTO;
import com.mobdata.pontocerto.dto.AlocacaoRequestDTO;
import com.mobdata.pontocerto.dto.AlocacaoResponseDTO;
import com.mobdata.pontocerto.dto.TrocaEscalaRequestDTO;
import com.mobdata.pontocerto.dto.TrocaEscalaResponseDTO;
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

    // Devolve o DTO (e não a entidade Alocacao) para não expor o senhaHash do usuário
    @PostMapping("/alocacoes")
    public ResponseEntity<AlocacaoResponseDTO> alocar(@Valid @RequestBody AlocacaoRequestDTO request) {
        AlocacaoResponseDTO alocacao = AlocacaoResponseDTO.fromEntity(alocacaoService.alocar(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(alocacao);
    }

    @PatchMapping("/alocacoes/{id}/encerrar")
    public ResponseEntity<AlocacaoResponseDTO> encerrar(@PathVariable UUID id, @RequestParam LocalDate dataFim) {
        return ResponseEntity.ok(AlocacaoResponseDTO.fromEntity(alocacaoService.encerrar(id, dataFim)));
    }

    @PostMapping("/alocacoes/{id}/trocar-escala")
    public ResponseEntity<TrocaEscalaResponseDTO> trocarEscala(
            @PathVariable UUID id,
            @Valid @RequestBody TrocaEscalaRequestDTO request
    ) {
        return ResponseEntity.ok(alocacaoService.trocarEscala(id, request));
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
