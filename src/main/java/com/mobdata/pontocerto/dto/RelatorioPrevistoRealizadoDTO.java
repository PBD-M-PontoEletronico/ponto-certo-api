package com.mobdata.pontocerto.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * W12 — relatório de previsto contra realizado.
 * "calculadoAte" é o último dia que entrou na conta: se o período pedido
 * passa de hoje, os dias futuros ficam de fora (ainda não aconteceram).
 */
public record RelatorioPrevistoRealizadoDTO(
        LocalDate inicio,
        LocalDate fim,
        LocalDate calculadoAte,
        AgrupamentoRelatorio agrupamento,
        List<RelatorioPeriodoDTO> periodos,
        List<RelatorioLinhaDTO> funcionarios,
        List<RelatorioLinhaDTO> setores,
        RelatorioLinhaDTO totalGeral
) {
}
