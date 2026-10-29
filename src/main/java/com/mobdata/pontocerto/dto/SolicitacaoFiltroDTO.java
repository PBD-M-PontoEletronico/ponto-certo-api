package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.StatusSolicitacao;
import com.mobdata.pontocerto.model.TipoSolicitacao;

import java.time.LocalDate;
import java.util.UUID;

public record SolicitacaoFiltroDTO(
        String usuarioNome,
        UUID empresaId,
        StatusSolicitacao status,
        TipoSolicitacao tipo,
        LocalDate dataDe,
        LocalDate dataAte
) {
}
