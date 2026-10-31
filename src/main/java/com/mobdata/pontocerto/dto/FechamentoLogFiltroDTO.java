package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.AcaoFechamento;

import java.util.UUID;

public record FechamentoLogFiltroDTO(
        UUID empresaId,
        Integer ano,
        Integer mes,
        AcaoFechamento acao
) {
}
