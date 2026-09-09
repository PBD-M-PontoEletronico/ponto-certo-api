package com.mobdata.pontocerto.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AlocacaoRequestDTO(
        @NotNull(message = "Funcionário é obrigatório")
        UUID usuarioId, // era funcionarioId

        @NotNull(message = "Setor é obrigatório")
        UUID setorId,

        @NotNull(message = "Data de início é obrigatória")
        LocalDate dataInicio,

        LocalDate dataFim
) {}