package com.mobdata.pontocerto.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AgendaTurnoDTO(
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        boolean atravessaMeiaNoite,
        UUID setorId,
        String setorNome,
        UUID escalaId,
        String escalaNome
) {
}
