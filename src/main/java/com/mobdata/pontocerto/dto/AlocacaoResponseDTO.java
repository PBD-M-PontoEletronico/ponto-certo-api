package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Alocacao;
import com.mobdata.pontocerto.model.Setor;

import java.time.LocalDate;
import java.util.UUID;

public record AlocacaoResponseDTO(

        UUID id,
        UsuarioResponseDTO usuario,
        Setor setor,
        LocalDate dataInicio,
        LocalDate dataFim
) {

    public static AlocacaoResponseDTO fromEntity(Alocacao alocacao) {
        return new AlocacaoResponseDTO(
                alocacao.getId(),
                UsuarioResponseDTO.fromEntity(alocacao.getUsuario()),
                alocacao.getSetor(),
                alocacao.getDataInicio(),
                alocacao.getDataFim()
        );
    }
}
