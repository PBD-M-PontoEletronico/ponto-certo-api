package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.FechamentoRequestDTO;
import com.mobdata.pontocerto.dto.FechamentoResponseDTO;
import com.mobdata.pontocerto.service.FechamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Só RH e superadmin (ver SecurityConfig).
@RestController
@RequestMapping("/fechamentos")
public class FechamentoController {

    @Autowired
    private FechamentoService fechamentoService;

    @GetMapping
    public ResponseEntity<List<FechamentoResponseDTO>> listar(@RequestParam(required = false) UUID empresaId) {
        return ResponseEntity.ok(fechamentoService.listar(empresaId).stream()
                .map(FechamentoResponseDTO::fromEntity)
                .toList());
    }

    @PostMapping
    public ResponseEntity<FechamentoResponseDTO> fechar(@Valid @RequestBody FechamentoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FechamentoResponseDTO.fromEntity(fechamentoService.fechar(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> reabrir(@PathVariable UUID id) {
        fechamentoService.reabrir(id);
        return ResponseEntity.noContent().build();
    }
}
