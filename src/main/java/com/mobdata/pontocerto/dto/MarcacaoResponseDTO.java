package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Marcacao;
import com.mobdata.pontocerto.model.MetodoConfirmacao;
import com.mobdata.pontocerto.model.StatusMarcacao;
import com.mobdata.pontocerto.model.TipoMarcacao;

import java.time.Instant;
import java.util.UUID;

public record MarcacaoResponseDTO(
        UUID id,
        UUID usuarioId,
        String usuarioNome,
        UUID registradoPorId,
        String registradoPorNome,
        UUID setorId,
        String setorNome,
        UUID empresaId,
        String empresaNome,
        TipoMarcacao tipo,
        Instant horaAparelho,
        Instant horaCorrigida,
        boolean offline,
        Instant recebidaEm,
        Double latitude,
        Double longitude,
        MetodoConfirmacao metodoConfirmacao,
        boolean temSelfie,
        StatusMarcacao status,
        String motivoRecusa,
        // W10 — marcação criada por ajuste aprovado (nulo = batida no aparelho)
        boolean ajuste,
        UUID solicitacaoId
) {
    public static MarcacaoResponseDTO fromEntity(Marcacao m) {
        var usuario = m.getUsuario();
        var registradoPor = m.getRegistradoPor();
        var setor = m.getSetor();
        var empresa = usuario.getEmpresa();
        return new MarcacaoResponseDTO(
                m.getId(),
                usuario.getId(), usuario.getNome(),
                registradoPor != null ? registradoPor.getId() : null,
                registradoPor != null ? registradoPor.getNome() : null,
                setor != null ? setor.getId() : null,
                setor != null ? setor.getNome() : null,
                empresa != null ? empresa.getId() : null,
                empresa != null ? empresa.getRazaoSocial() : null,
                m.getTipo(),
                m.getHoraAparelho(),
                m.getHoraCorrigida(),
                m.isOffline(),
                m.getRecebidaEm(),
                m.getLatitude(),
                m.getLongitude(),
                m.getMetodoConfirmacao(),
                m.getSelfieArquivo() != null,
                m.getStatus(),
                m.getMotivoRecusa(),
                m.isAjuste(),
                m.getSolicitacao() != null ? m.getSolicitacao().getId() : null
        );
    }
}