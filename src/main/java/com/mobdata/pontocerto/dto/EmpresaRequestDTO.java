package com.mobdata.pontocerto.dto;

import jakarta.validation.constraints.NotBlank;

public record EmpresaRequestDTO(
        @NotBlank(message = "Razão social é obrigatória")
        String razaoSocial,
        String contato
) {
}
