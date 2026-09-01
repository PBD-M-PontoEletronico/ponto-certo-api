package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.EmpresaRequestDTO;
import com.mobdata.pontocerto.model.Empresa;
import com.mobdata.pontocerto.service.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {
    @Autowired
    private EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<Empresa> cadastrar(@Valid @RequestBody EmpresaRequestDTO request) {
        Empresa empresa = empresaService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(empresa);
    }

    @GetMapping
    public ResponseEntity<List<Empresa>> listar() {
        return ResponseEntity.ok(empresaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Empresa> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(empresaService.buscarPorId(id));
    }

    @PatchMapping("/{id}/situacao")
    public ResponseEntity<Empresa> atualizarSituacao(
            @PathVariable UUID id,
            @RequestParam boolean ativa
    ) {
        return ResponseEntity.ok(empresaService.atualizarSituacao(id, ativa));
    }
}
