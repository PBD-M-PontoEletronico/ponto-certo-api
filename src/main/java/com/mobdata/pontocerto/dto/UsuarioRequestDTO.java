package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UsuarioRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        @NotBlank(message = "Usuário é obrigatório")
        String usuario,
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha,
        @NotNull(message = "Perfil é obrigatório")
        Perfil perfil,
        UUID empresaId
) {
}
