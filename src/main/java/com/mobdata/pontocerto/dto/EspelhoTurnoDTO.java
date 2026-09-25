package com.mobdata.pontocerto.dto;

import java.time.LocalTime;
import java.util.UUID;

public record EspelhoTurnoDTO(
        LocalTime horaInicio,
        LocalTime horaFim,
        boolean atravessaMeiaNoite,
        UUID setorId,
        String setorNome,
        String escalaNome
) {
}
