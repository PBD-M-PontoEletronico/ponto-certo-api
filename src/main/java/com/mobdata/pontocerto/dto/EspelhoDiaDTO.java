package com.mobdata.pontocerto.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Um dia do espelho. "descricao" traz o motivo quando o dia é FERIADO
 * (nome do feriado) ou AFASTAMENTO (tipo). Nesses dois casos "turnos" vem
 * vazio: não há turno esperado, logo não há falta nem atraso.
 */
public record EspelhoDiaDTO(
        LocalDate data,
        SituacaoDia situacao,
        String descricao,
        List<EspelhoTurnoDTO> turnos
) {
}
