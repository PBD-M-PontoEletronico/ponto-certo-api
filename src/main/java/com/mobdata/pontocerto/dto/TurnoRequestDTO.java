package com.mobdata.pontocerto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record TurnoRequestDTO(
        @NotNull(message = "Hora de início é obrigatória")
        LocalTime horaInicio,
        @NotNull(message = "Hora de fim é obrigatória")
        LocalTime horaFim,
        @Min(value = 0, message = "Intervalo não pode ser negativo")
        int intervaloMinutos
) {
}