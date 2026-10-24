package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.PoliticaForaPerimetro;

import java.util.UUID;

public record SetorFiltroDTO(
        String nome,
        UUID empresaId,
        Boolean exigirSelfie,
        Boolean ignorarLocalizacao,
        PoliticaForaPerimetro politicaForaPerimetro
) {
}
