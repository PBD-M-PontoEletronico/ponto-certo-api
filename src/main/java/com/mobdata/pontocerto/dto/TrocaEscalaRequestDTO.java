package com.mobdata.pontocerto.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record TrocaEscalaRequestDTO(
        @NotNull(message = "Escala é obrigatória")
        UUID escalaId,

        // Vazio = continua no mesmo setor
        UUID setorId,

        // Primeiro dia da nova escala. A alocação atual termina no dia anterior.
        @NotNull(message = "Data da troca é obrigatória")
        LocalDate dataTroca,

        // Vazio = a nova alocação vai até o fim que a atual tinha
        LocalDate dataFim
) {
}
