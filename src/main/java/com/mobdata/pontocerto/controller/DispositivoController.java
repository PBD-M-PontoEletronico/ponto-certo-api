package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.DispositivoFiltroDTO;
import com.mobdata.pontocerto.dto.DispositivoResponseDTO;
import com.mobdata.pontocerto.dto.PaginaDTO;
import com.mobdata.pontocerto.model.TipoDispositivo;
import com.mobdata.pontocerto.service.DispositivoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/dispositivos")
public class DispositivoController {

    @Autowired
    private DispositivoService dispositivoService;

    @GetMapping
    public ResponseEntity<PaginaDTO<DispositivoResponseDTO>> buscar(
            @RequestParam(required = false) String usuarioNome,
            @RequestParam(required = false) TipoDispositivo tipo,
            @RequestParam(required = false) UUID setorId,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ultimoAcessoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ultimoAcessoAte,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVinculoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVinculoAte,
            @PageableDefault(size = 10, sort = "ultimoAcesso", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        DispositivoFiltroDTO filtro = new DispositivoFiltroDTO(
                usuarioNome, tipo, setorId, ativo, empresaId,
                ultimoAcessoDe, ultimoAcessoAte, dataVinculoDe, dataVinculoAte
        );
        return ResponseEntity.ok(dispositivoService.buscar(filtro, pageable));
    }

    @PatchMapping("/{id}/revogar")
    public ResponseEntity<Void> revogar(@PathVariable UUID id) {
        dispositivoService.revogar(id);
        return ResponseEntity.noContent().build();
    }
}