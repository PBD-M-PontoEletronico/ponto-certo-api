package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.FeriadoRequestDTO;
import com.mobdata.pontocerto.dto.FeriadoResponseDTO;
import com.mobdata.pontocerto.service.FeriadoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/feriados")
public class FeriadoController {

    @Autowired
    private FeriadoService feriadoService;

    @PostMapping
    public ResponseEntity<FeriadoResponseDTO> cadastrar(@Valid @RequestBody FeriadoRequestDTO request) {
        FeriadoResponseDTO feriado = FeriadoResponseDTO.fromEntity(feriadoService.cadastrar(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(feriado);
    }

    @GetMapping
    public ResponseEntity<List<FeriadoResponseDTO>> listar(
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) Integer ano
    ) {
        List<FeriadoResponseDTO> feriados = feriadoService.listar(empresaId, ano)
                .stream()
                .map(FeriadoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(feriados);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        feriadoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
