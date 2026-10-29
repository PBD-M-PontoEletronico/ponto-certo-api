package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.FechamentoPeriodo;

import java.time.Instant;
import java.util.UUID;

public record FechamentoResponseDTO(
        UUID id,
        UUID empresaId,
        String mes, // "AAAA-MM"
        Instant fechadoEm,
        String fechadoPorNome
) {
    public static FechamentoResponseDTO fromEntity(FechamentoPeriodo f) {
        return new FechamentoResponseDTO(
                f.getId(),
                f.getEmpresa().getId(),
                String.format("%04d-%02d", f.getAno(), f.getMes()),
                f.getFechadoEm(),
                f.getFechadoPor().getNome()
        );
    }
}
