package com.mobdata.pontocerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

// mes no formato "AAAA-MM". empresaId só é usado pelo superadmin.
public record FechamentoRequestDTO(
        UUID empresaId,

        @NotBlank(message = "Informe o mês")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Mês deve estar no formato AAAA-MM")
        String mes
) {
}
