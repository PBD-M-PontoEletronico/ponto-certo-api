package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.Usuario;

import java.util.UUID;

public record UsuarioResponseDTO(
        UUID id,
        String nome,
        String usuario,
        Perfil perfil,
        UUID empresaId,
        String matricula,
        String cargo
) {
    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getUsuario(),
                usuario.getPerfil(),
                usuario.getEmpresa().getId(),
                usuario.getMatricula(),
                usuario.getCargo()
        );
    }
}
