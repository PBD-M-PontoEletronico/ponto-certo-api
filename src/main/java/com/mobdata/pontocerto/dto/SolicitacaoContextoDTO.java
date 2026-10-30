package com.mobdata.pontocerto.dto;

import java.util.List;

/**
 * O que o gestor precisa ver para decidir um pedido: o que estava previsto
 * para o funcionário naquele dia e o que de fato foi batido (marcações
 * originais e ajustes já aprovados), além de o mês já estar fechado ou não.
 */
public record SolicitacaoContextoDTO(
        SituacaoDia situacao,
        String descricao,
        List<EspelhoTurnoDTO> turnosPrevistos,
        List<MarcacaoResponseDTO> marcacoesDoDia,
        boolean periodoFechado
) {
}
