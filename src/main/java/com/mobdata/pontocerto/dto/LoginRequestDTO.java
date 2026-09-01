package com.mobdata.pontocerto.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "Usuário é obrigatório")
        String usuario,
        @NotBlank(message = "Senha é obrigatório")
        String senha
) {
}
