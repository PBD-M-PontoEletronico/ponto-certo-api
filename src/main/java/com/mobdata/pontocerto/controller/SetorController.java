package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.PaginaDTO;
import com.mobdata.pontocerto.dto.SetorFiltroDTO;
import com.mobdata.pontocerto.dto.SetorRequestDTO;
import com.mobdata.pontocerto.model.PoliticaForaPerimetro;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.service.SetorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/setores")
public class SetorController {

    @Autowired
    private SetorService setorService;

    @PostMapping
    public ResponseEntity<Setor> cadastrar(@Valid @RequestBody SetorRequestDTO request) {
        Setor setor = setorService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(setor);
    }

    @GetMapping
    public ResponseEntity<List<Setor>> listar(@RequestParam(required = false) UUID empresaId) {
        return ResponseEntity.ok(setorService.listar(empresaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Setor> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(setorService.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        setorService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/busca")
    public ResponseEntity<PaginaDTO<Setor>> buscar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) Boolean exigirSelfie,
            @RequestParam(required = false) Boolean ignorarLocalizacao,
            @RequestParam(required = false) PoliticaForaPerimetro politicaForaPerimetro,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable
    ) {
        var filtro = new SetorFiltroDTO(nome, empresaId, exigirSelfie, ignorarLocalizacao, politicaForaPerimetro);
        return ResponseEntity.ok(setorService.buscar(filtro, pageable));
    }
}