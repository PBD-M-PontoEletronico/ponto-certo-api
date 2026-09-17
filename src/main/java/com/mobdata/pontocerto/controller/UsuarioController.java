package com.mobdata.pontocerto.controller;

import com.mobdata.pontocerto.dto.PaginaDTO;
import com.mobdata.pontocerto.dto.UsuarioFiltroDTO;
import com.mobdata.pontocerto.dto.UsuarioRequestDTO;
import com.mobdata.pontocerto.dto.UsuarioResponseDTO;
import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.service.UsuarioService;
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
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<Usuario> cadastrar(@Valid @RequestBody UsuarioRequestDTO request) {
        Usuario usuario = usuarioService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listar(@RequestParam(required = false) UUID empresaId) {
        return ResponseEntity.ok(usuarioService.listar(empresaId));
    }

    @GetMapping("/busca")
    public ResponseEntity<PaginaDTO<UsuarioResponseDTO>> buscar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) Perfil perfil,
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String cargo,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable
    ) {
        UsuarioFiltroDTO filtro = new UsuarioFiltroDTO(nome, usuario, perfil, empresaId, matricula, cargo);
        return ResponseEntity.ok(usuarioService.buscar(filtro, pageable));
    }
}
