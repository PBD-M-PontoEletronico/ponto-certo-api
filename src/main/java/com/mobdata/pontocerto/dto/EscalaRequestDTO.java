package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.ModelEscala;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record EscalaRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,
        @NotNull(message = "Modelo é obrigatório")
        ModelEscala modelo,
        @NotEmpty(message = "A escala precisa de ao menos um turno")
        List<TurnoRequestDTO> turnos,
        LocalDate dataReferencia,
        Set<Integer> diasSemana,
        UUID empresaId
) {
}