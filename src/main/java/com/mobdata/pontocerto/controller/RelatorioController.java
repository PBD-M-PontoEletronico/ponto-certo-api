package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.AgrupamentoRelatorio;
import com.mobdata.pontocerto.dto.RelatorioPrevistoRealizadoDTO;
import com.mobdata.pontocerto.service.RelatorioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

// W12 — Relatórios. Só RH e superadmin (ver SecurityConfig).
@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;

    // Ex.: GET /relatorios/previsto-realizado?inicio=2026-10-01&fim=2026-10-31&agrupamento=SEMANA&setorId=...
    @GetMapping("/previsto-realizado")
    public ResponseEntity<RelatorioPrevistoRealizadoDTO> previstoRealizado(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "MES") AgrupamentoRelatorio agrupamento,
            @RequestParam(required = false) UUID setorId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) UUID empresaId
    ) {
        return ResponseEntity.ok(
                relatorioService.previstoRealizado(inicio, fim, agrupamento, setorId, usuarioId, empresaId));
    }

    // Mesmo relatório em CSV para abrir no Excel.
    @GetMapping("/previsto-realizado/csv")
    public ResponseEntity<byte[]> previstoRealizadoCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "MES") AgrupamentoRelatorio agrupamento,
            @RequestParam(required = false) UUID setorId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) UUID empresaId
    ) {
        byte[] csv = relatorioService.previstoRealizadoCsv(inicio, fim, agrupamento, setorId, usuarioId, empresaId);
        String nomeArquivo = "previsto-realizado_" + inicio + "_" + fim + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }
}
