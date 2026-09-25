package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.Tema;

import java.util.List;
import java.util.UUID;

public record LoginResponseDTO(
        String token,
        String nome,
        Perfil perfil,
        UUID empresaId,
        List<UUID> setorsId,
        Tema tema
) {
}
