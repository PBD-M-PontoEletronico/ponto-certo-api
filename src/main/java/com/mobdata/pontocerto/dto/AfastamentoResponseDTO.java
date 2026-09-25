package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.Afastamento;
import com.mobdata.pontocerto.model.TipoAfastamento;

import java.time.LocalDate;
import java.util.UUID;

public record AfastamentoResponseDTO(
        UUID id,
        UsuarioResponseDTO usuario,
        TipoAfastamento tipo,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean temAnexo,
        String anexoNome
) {
    public static AfastamentoResponseDTO fromEntity(Afastamento afastamento) {
        return new AfastamentoResponseDTO(
                afastamento.getId(),
                UsuarioResponseDTO.fromEntity(afastamento.getUsuario()),
                afastamento.getTipo(),
                afastamento.getDataInicio(),
                afastamento.getDataFim(),
                afastamento.getAnexoArquivo() != null,
                afastamento.getAnexoNomeOriginal()
        );
    }
}
