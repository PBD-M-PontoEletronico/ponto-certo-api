package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.*;
import com.mobdata.pontocerto.model.StatusMarcacao;
import com.mobdata.pontocerto.model.TipoMarcacao;
import com.mobdata.pontocerto.service.MarcacaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/marcacoes")
public class MarcacaoController {

    @Autowired
    private MarcacaoService marcacaoService;

    // multipart/form-data: campos do formulário + foto opcional em "foto".
    // O "id" vem do aparelho (ver MarcacaoForm) — reenvio de sincronização
    // offline com o mesmo id não duplica, o service trata isso.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MarcacaoResponseDTO> registrar(@Valid @ModelAttribute MarcacaoForm form) {
        MarcacaoResponseDTO resultado = marcacaoService.registrar(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @GetMapping
    public ResponseEntity<PaginaDTO<MarcacaoResponseDTO>> buscar(
            @RequestParam(required = false) String usuarioNome,
            @RequestParam(required = false) UUID setorId,
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) StatusMarcacao status,
            @RequestParam(required = false) TipoMarcacao tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAte,
            @PageableDefault(size = 10, sort = "horaAparelho", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        var filtro = new MarcacaoFiltroDTO(usuarioNome, setorId, empresaId, status, tipo, dataDe, dataAte);
        return ResponseEntity.ok(marcacaoService.buscar(filtro, pageable));
    }

    @PatchMapping("/{id}/revisar")
    public ResponseEntity<MarcacaoResponseDTO> revisar(@PathVariable UUID id, @RequestBody RevisarMarcacaoDTO dto) {
        return ResponseEntity.ok(marcacaoService.revisar(id, dto));
    }

    @GetMapping("/{id}/selfie")
    public ResponseEntity<byte[]> selfie(@PathVariable UUID id) {
        var selfie = marcacaoService.obterSelfie(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(selfie.contentType()))
                .body(selfie.bytes());
    }
}