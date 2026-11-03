package com.mobdata.pontocerto.dto;

import java.util.List;
import java.util.UUID;

/**
 * W12 — uma linha do relatório: um funcionário, um setor ou o total geral.
 * "porPeriodo" vem na mesma ordem da lista "periodos" do relatório.
 * Para setor, "id" null = dias sem setor definido (funcionário sem alocação
 * naquele dia que mesmo assim bateu ponto).
 */
public record RelatorioLinhaDTO(
        UUID id,
        String nome,
        String detalhe,
        RelatorioIndicadoresDTO total,
        List<RelatorioIndicadoresDTO> porPeriodo
) {
}
