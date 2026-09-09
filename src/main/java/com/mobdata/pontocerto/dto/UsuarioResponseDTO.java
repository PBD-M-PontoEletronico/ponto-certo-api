package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Perfil;

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
}
