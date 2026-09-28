package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.TipoDispositivo;

import java.time.LocalDate;
import java.util.UUID;

public record DispositivoFiltroDTO(
        String usuarioNome,
        TipoDispositivo tipo,
        UUID setorId,
        Boolean ativo,
        UUID empresaId,
        LocalDate ultimoAcessoDe,
        LocalDate ultimoAcessoAte,
        LocalDate dataVinculoDe,
        LocalDate dataVinculoAte
) {
}