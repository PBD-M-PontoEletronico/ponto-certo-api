package com.mobdata.pontocerto.dto;

public record TrocaEscalaResponseDTO(
        AlocacaoResponseDTO encerrada,
        AlocacaoResponseDTO nova
) {
}
