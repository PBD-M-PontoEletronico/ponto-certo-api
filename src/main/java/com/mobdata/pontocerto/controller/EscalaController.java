package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.EscalaRequestDTO;
import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.service.EscalaService;
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
@RequestMapping("/escalas")
public class EscalaController {

    @Autowired
    private EscalaService escalaService;

    @PostMapping
    public ResponseEntity<Escala> cadastrar(@Valid @RequestBody EscalaRequestDTO request) {
        Escala escala = escalaService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(escala);
    }

    @GetMapping
    public ResponseEntity<List<Escala>> listar(@RequestParam(required = false) UUID empresaId) {
        return ResponseEntity.ok(escalaService.listar(empresaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Escala> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(escalaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Escala> atualizar(@PathVariable UUID id, @Valid @RequestBody EscalaRequestDTO request) {
        return ResponseEntity.ok(escalaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        escalaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/previsao")
    public ResponseEntity<List<EscalaService.PrevisaoDia>> previsao(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(defaultValue = "14") int dias
    ) {
        return ResponseEntity.ok(escalaService.gerarPrevisao(id, dataInicio, dias));
    }
}