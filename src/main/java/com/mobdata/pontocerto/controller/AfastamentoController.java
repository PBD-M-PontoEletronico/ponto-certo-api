package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.AfastamentoForm;
import com.mobdata.pontocerto.dto.AfastamentoResponseDTO;
import com.mobdata.pontocerto.service.AfastamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/afastamentos")
public class AfastamentoController {

    @Autowired
    private AfastamentoService afastamentoService;

    // multipart/form-data: campos do formulário + arquivo opcional em "anexo"
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AfastamentoResponseDTO> cadastrar(@Valid @ModelAttribute AfastamentoForm form) {
        AfastamentoResponseDTO afastamento = AfastamentoResponseDTO.fromEntity(afastamentoService.cadastrar(form));
        return ResponseEntity.status(HttpStatus.CREATED).body(afastamento);
    }

    @GetMapping
    public ResponseEntity<List<AfastamentoResponseDTO>> listar(
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) UUID usuarioId
    ) {
        List<AfastamentoResponseDTO> afastamentos = afastamentoService.listar(empresaId, usuarioId)
                .stream()
                .map(AfastamentoResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(afastamentos);
    }

    @GetMapping("/{id}/anexo")
    public ResponseEntity<byte[]> baixarAnexo(@PathVariable UUID id) {
        AfastamentoService.AnexoDownload anexo = afastamentoService.baixarAnexo(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.tipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(anexo.nome(), StandardCharsets.UTF_8).build().toString())
                .body(anexo.conteudo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        afastamentoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
