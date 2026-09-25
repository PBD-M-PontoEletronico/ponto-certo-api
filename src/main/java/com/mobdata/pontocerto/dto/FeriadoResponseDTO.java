package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Feriado;

import java.time.LocalDate;
import java.util.UUID;

public record FeriadoResponseDTO(
        UUID id,
        LocalDate data,
        String descricao,
        UUID empresaId,
        UUID setorId,
        String setorNome
) {
    public static FeriadoResponseDTO fromEntity(Feriado feriado) {
        return new FeriadoResponseDTO(
                feriado.getId(),
                feriado.getData(),
                feriado.getDescricao(),
                feriado.getEmpresa().getId(),
                feriado.getSetor() != null ? feriado.getSetor().getId() : null,
                feriado.getSetor() != null ? feriado.getSetor().getNome() : null
        );
    }
}
