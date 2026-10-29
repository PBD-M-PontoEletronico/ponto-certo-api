package com.mobdata.pontocerto.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Um dia do espelho. "descricao" traz o motivo quando o dia é FERIADO
 * (nome do feriado) ou AFASTAMENTO (tipo). Nesses dois casos "turnos" vem
 * vazio: não há turno esperado, logo não há falta nem atraso.
 *
 * W10 — "marcacoes" traz o que aconteceu no dia: as batidas originais
 * (ajuste = false) e as criadas por ajuste aprovado (ajuste = true), para
 * mostrar lado a lado. "justificativas" traz as pendentes e as aprovadas.
 * Só o espelho do mês preenche essas listas; a agenda da abertura do app
 * manda as duas vazias.
 */
public record EspelhoDiaDTO(
        LocalDate data,
        SituacaoDia situacao,
        String descricao,
        List<EspelhoTurnoDTO> turnos,
        List<EspelhoMarcacaoDTO> marcacoes,
        List<EspelhoJustificativaDTO> justificativas
) {
    // Construtor antigo (só a escala do dia) — continua valendo para quem
    // não precisa das marcações, como a abertura do app.
    public EspelhoDiaDTO(LocalDate data, SituacaoDia situacao, String descricao, List<EspelhoTurnoDTO> turnos) {
        this(data, situacao, descricao, turnos, List.of(), List.of());
    }

    public EspelhoDiaDTO comRealizado(List<EspelhoMarcacaoDTO> marcacoes, List<EspelhoJustificativaDTO> justificativas) {
        return new EspelhoDiaDTO(data, situacao, descricao, turnos, marcacoes, justificativas);
    }
}
