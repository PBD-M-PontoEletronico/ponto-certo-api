package com.mobdata.pontocerto.dto;

import java.time.LocalDate;

// W12 — uma fatia do período do relatório (uma semana ou um mês), já
// recortada pelo início/fim pedidos. Ex.: "21/09 a 27/09" ou "10/2026".
public record RelatorioPeriodoDTO(
        LocalDate inicio,
        LocalDate fim,
        String rotulo
) {
}
