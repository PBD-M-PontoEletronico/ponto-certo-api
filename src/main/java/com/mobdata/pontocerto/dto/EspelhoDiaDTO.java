package com.mobdata.pontocerto.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Um dia do espelho. "minutosTrabalhados"/"minutosAtraso"/"saldoDoDiaMinutos"
 * ficam null quando o dia ainda está em andamento (bateu ENTRADA mas não
 * SAIDA) — não dá pra fechar conta de um dia que a pessoa ainda não terminou.
 * "saldoAcumuladoMinutos" é o banco de horas corrido até este dia, somando o
 * saldo congelado do último mês fechado com os dias já calculados deste mês.
 */
public record EspelhoDiaDTO(
        LocalDate data,
        SituacaoDia situacao,
        String descricao,
        List<EspelhoTurnoDTO> turnos,
        List<EspelhoMarcacaoDTO> marcacoes,
        List<EspelhoJustificativaDTO> justificativas,
        int minutosPrevistos,
        Integer minutosTrabalhados,
        Integer minutosAtraso,
        Integer saldoDoDiaMinutos,
        int saldoAcumuladoMinutos
) {
    public EspelhoDiaDTO(LocalDate data, SituacaoDia situacao, String descricao,
                         List<EspelhoTurnoDTO> turnos, int minutosPrevistos) {
        this(data, situacao, descricao, turnos, List.of(), List.of(), minutosPrevistos, null, null, null, 0);
    }

    public EspelhoDiaDTO comRealizado(List<EspelhoMarcacaoDTO> marcacoes, List<EspelhoJustificativaDTO> justificativas) {
        return new EspelhoDiaDTO(data, situacao, descricao, turnos, marcacoes, justificativas,
                minutosPrevistos, minutosTrabalhados, minutosAtraso, saldoDoDiaMinutos, saldoAcumuladoMinutos);
    }

    public EspelhoDiaDTO comHoras(Integer minutosTrabalhados, Integer minutosAtraso,
                                  Integer saldoDoDiaMinutos, int saldoAcumuladoMinutos) {
        return new EspelhoDiaDTO(data, situacao, descricao, turnos, marcacoes, justificativas,
                minutosPrevistos, minutosTrabalhados, minutosAtraso, saldoDoDiaMinutos, saldoAcumuladoMinutos);
    }
}