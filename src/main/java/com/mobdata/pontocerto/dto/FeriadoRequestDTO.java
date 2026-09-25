package com.mobdata.pontocerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record FeriadoRequestDTO(
        @NotNull(message = "Data é obrigatória")
        LocalDate data,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 120, message = "Descrição deve ter no máximo 120 caracteres")
        String descricao,

        // Vazio = feriado da empresa inteira; preenchido = só daquele setor
        UUID setorId,

        // Só o SUPERADMIN precisa informar (os demais usam a empresa do próprio token)
        UUID empresaId
) {
}
