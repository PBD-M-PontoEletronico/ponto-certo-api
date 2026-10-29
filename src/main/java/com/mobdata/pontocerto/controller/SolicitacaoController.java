package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.*;
import com.mobdata.pontocerto.model.StatusSolicitacao;
import com.mobdata.pontocerto.model.TipoSolicitacao;
import com.mobdata.pontocerto.service.AfastamentoService;
import com.mobdata.pontocerto.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * W10 — pedidos de justificativa e de ajuste.
 *
 * POST   /solicitacoes                  qualquer usuário logado (multipart)
 * GET    /solicitacoes/minhas           o próprio usuário (app)
 * GET    /solicitacoes                  fila do gestor/RH (pendentes primeiro)
 * GET    /solicitacoes/{id}             dono ou gestor/RH da empresa
 * GET    /solicitacoes/{id}/contexto    previsto x batido no dia (gestor/RH)
 * GET    /solicitacoes/{id}/anexo       dono ou gestor/RH da empresa
 * PATCH  /solicitacoes/{id}/analisar    gestor/RH — aprova ou recusa com parecer
 */
@RestController
@RequestMapping("/solicitacoes")
public class SolicitacaoController {

    @Autowired
    private SolicitacaoService solicitacaoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SolicitacaoResponseDTO> criar(@Valid @ModelAttribute SolicitacaoForm form) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SolicitacaoResponseDTO.fromEntity(solicitacaoService.criar(form)));
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<SolicitacaoResponseDTO>> minhas() {
        return ResponseEntity.ok(solicitacaoService.meus().stream()
                .map(SolicitacaoResponseDTO::fromEntity)
                .toList());
    }

    @GetMapping
    public ResponseEntity<PaginaDTO<SolicitacaoResponseDTO>> fila(
            @RequestParam(required = false) String usuarioNome,
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) StatusSolicitacao status,
            @RequestParam(required = false) TipoSolicitacao tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAte,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var filtro = new SolicitacaoFiltroDTO(usuarioNome, empresaId, status, tipo, dataDe, dataAte);
        return ResponseEntity.ok(solicitacaoService.fila(filtro, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoResponseDTO> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(SolicitacaoResponseDTO.fromEntity(solicitacaoService.buscar(id)));
    }

    @GetMapping("/{id}/contexto")
    public ResponseEntity<SolicitacaoContextoDTO> contexto(@PathVariable UUID id) {
        return ResponseEntity.ok(solicitacaoService.contexto(id));
    }

    @GetMapping("/{id}/anexo")
    public ResponseEntity<byte[]> baixarAnexo(@PathVariable UUID id) {
        AfastamentoService.AnexoDownload anexo = solicitacaoService.baixarAnexo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.tipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(anexo.nome(), StandardCharsets.UTF_8).build().toString())
                .body(anexo.conteudo());
    }

    @PatchMapping("/{id}/analisar")
    public ResponseEntity<SolicitacaoResponseDTO> analisar(@PathVariable UUID id,
                                                           @Valid @RequestBody AnalisarSolicitacaoDTO dto) {
        return ResponseEntity.ok(SolicitacaoResponseDTO.fromEntity(solicitacaoService.analisar(id, dto)));
    }
}
