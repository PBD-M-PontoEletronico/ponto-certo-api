package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Perfil;

import java.util.UUID;

public record LoginResponseDTO(
        String token,
        String nome,
        Perfil perfil,
        UUID empresaId
) {
}
