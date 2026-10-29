package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Solicitacao;
import com.mobdata.pontocerto.model.StatusSolicitacao;
import com.mobdata.pontocerto.model.TipoOcorrencia;

import java.time.Instant;
import java.util.UUID;

// Justificativa (pendente ou aprovada) de um dia do espelho.
public record EspelhoJustificativaDTO(
        UUID solicitacaoId,
        TipoOcorrencia ocorrencia,
        StatusSolicitacao status,
        String parecer,
        String analisadoPorNome,
        Instant analisadoEm
) {
    public static EspelhoJustificativaDTO fromEntity(Solicitacao s) {
        return new EspelhoJustificativaDTO(
                s.getId(),
                s.getOcorrencia(),
                s.getStatus(),
                s.getParecer(),
                s.getAnalisadoPor() != null ? s.getAnalisadoPor().getNome() : null,
                s.getAnalisadoEm()
        );
    }
}
