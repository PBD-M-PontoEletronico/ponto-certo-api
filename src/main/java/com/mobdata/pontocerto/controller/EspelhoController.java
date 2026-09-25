package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.EspelhoDiaDTO;
import com.mobdata.pontocerto.service.EspelhoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
public class EspelhoController {

    @Autowired
    private EspelhoService espelhoService;

    // Ex.: GET /usuarios/{id}/espelho?mes=2026-09
    @GetMapping("/usuarios/{id}/espelho")
    public ResponseEntity<List<EspelhoDiaDTO>> espelhoDoMes(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes
    ) {
        return ResponseEntity.ok(espelhoService.espelhoDoMes(id, mes));
    }
}
