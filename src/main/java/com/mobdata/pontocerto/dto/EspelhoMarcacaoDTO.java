package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Marcacao;
import com.mobdata.pontocerto.model.StatusMarcacao;
import com.mobdata.pontocerto.model.TipoMarcacao;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma marcação no espelho. "ajuste" separa as duas colunas mostradas lado a
 * lado: false = batida no aparelho (original); true = criada por ajuste
 * aprovado — aí vem junto quem aprovou e quando.
 */
public record EspelhoMarcacaoDTO(
        UUID id,
        TipoMarcacao tipo,
        Instant hora,
        StatusMarcacao status,
        boolean offline,
        boolean ajuste,
        UUID solicitacaoId,
        String aprovadoPorNome,
        Instant aprovadoEm
) {
    public static EspelhoMarcacaoDTO fromEntity(Marcacao m) {
        var pedido = m.getSolicitacao();
        return new EspelhoMarcacaoDTO(
                m.getId(),
                m.getTipo(),
                m.getHoraCorrigida(),
                m.getStatus(),
                m.isOffline(),
                pedido != null,
                pedido != null ? pedido.getId() : null,
                pedido != null && pedido.getAnalisadoPor() != null ? pedido.getAnalisadoPor().getNome() : null,
                pedido != null ? pedido.getAnalisadoEm() : null
        );
    }
}
