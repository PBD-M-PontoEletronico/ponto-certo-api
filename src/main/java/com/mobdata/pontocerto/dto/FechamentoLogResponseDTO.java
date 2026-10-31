package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.AcaoFechamento;
import com.mobdata.pontocerto.model.FechamentoLog;

import java.time.Instant;

public record FechamentoLogResponseDTO(
        int ano,
        int mes,
        AcaoFechamento acao,
        String empresaNome,
        String usuarioNome,
        Instant quando
) {
    public static FechamentoLogResponseDTO fromEntity(FechamentoLog log) {
        return new FechamentoLogResponseDTO(
                log.getAno(),
                log.getMes(),
                log.getAcao(),
                log.getEmpresa().getRazaoSocial(),
                log.getUsuario().getNome(),
                log.getQuando()
        );
    }
}