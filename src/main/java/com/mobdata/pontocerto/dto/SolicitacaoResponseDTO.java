package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Solicitacao;
import com.mobdata.pontocerto.model.StatusSolicitacao;
import com.mobdata.pontocerto.model.TipoMarcacao;
import com.mobdata.pontocerto.model.TipoOcorrencia;
import com.mobdata.pontocerto.model.TipoSolicitacao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SolicitacaoResponseDTO(
        UUID id,
        TipoSolicitacao tipo,
        StatusSolicitacao status,

        UUID usuarioId,
        String usuarioNome,
        String usuarioMatricula,
        UUID empresaId,
        String empresaNome,

        // quem pediu e quando
        UUID solicitanteId,
        String solicitanteNome,
        Instant criadoEm,

        LocalDate data,
        TipoOcorrencia ocorrencia,
        TipoMarcacao tipoMarcacao,
        LocalTime horaPretendida,
        String motivo,
        boolean temAnexo,
        String anexoNome,

        // quem analisou, quando e o que disse
        String parecer,
        UUID analisadoPorId,
        String analisadoPorNome,
        Instant analisadoEm,

        UUID marcacaoGeradaId
) {
    public static SolicitacaoResponseDTO fromEntity(Solicitacao s) {
        var usuario = s.getUsuario();
        var empresa = usuario.getEmpresa();
        var solicitante = s.getSolicitante();
        var analista = s.getAnalisadoPor();
        return new SolicitacaoResponseDTO(
                s.getId(),
                s.getTipo(),
                s.getStatus(),
                usuario.getId(),
                usuario.getNome(),
                usuario.getMatricula(),
                empresa != null ? empresa.getId() : null,
                empresa != null ? empresa.getRazaoSocial() : null,
                solicitante.getId(),
                solicitante.getNome(),
                s.getCriadoEm(),
                s.getData(),
                s.getOcorrencia(),
                s.getTipoMarcacao(),
                s.getHoraPretendida(),
                s.getMotivo(),
                s.getAnexoArquivo() != null,
                s.getAnexoNomeOriginal(),
                s.getParecer(),
                analista != null ? analista.getId() : null,
                analista != null ? analista.getNome() : null,
                s.getAnalisadoEm(),
                s.getMarcacaoGerada() != null ? s.getMarcacaoGerada().getId() : null
        );
    }
}
