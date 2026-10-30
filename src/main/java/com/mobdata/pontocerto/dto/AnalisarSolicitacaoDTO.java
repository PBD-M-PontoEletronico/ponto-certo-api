package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.StatusSolicitacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// decisao só pode ser APROVADA ou RECUSADA (conferido no service).
// O parecer é obrigatório nos dois casos.
public record AnalisarSolicitacaoDTO(
        @NotNull(message = "Informe se o pedido foi aprovado ou recusado")
        StatusSolicitacao decisao,

        @NotBlank(message = "O parecer é obrigatório")
        @Size(max = 1000, message = "O parecer deve ter no máximo 1000 caracteres")
        String parecer
) {
}
