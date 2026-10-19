package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.StatusMarcacao;
import com.mobdata.pontocerto.model.TipoMarcacao;

import java.time.LocalDate;
import java.util.UUID;

public record MarcacaoFiltroDTO(
        String usuarioNome,
        UUID setorId,
        UUID empresaId,
        StatusMarcacao status,
        TipoMarcacao tipo,
        LocalDate dataDe,
        LocalDate dataAte
) {
}