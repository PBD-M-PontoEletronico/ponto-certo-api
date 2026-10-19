package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.StatusMarcacao;

// novoStatus só pode ser VALIDA ou RECUSADA — validado no service.
public record RevisarMarcacaoDTO(
        StatusMarcacao novoStatus,
        String motivo
) {
}