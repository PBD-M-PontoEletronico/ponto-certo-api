package com.mobdata.pontocerto.dto;

import java.util.List;

/**
 * W12 — os números do previsto contra realizado de uma linha (funcionário,
 * setor ou total) num período. Tudo em minutos para não perder precisão;
 * a web formata em horas.
 *
 * - diferencaMinutos = realizado - previsto (negativo: faltou hora; positivo: sobrou)
 * - minutosExtras: soma do que passou do previsto, dia a dia (dia de folga
 *   trabalhado conta inteiro como extra)
 * - atrasos/faltas: justificado = tem justificativa APROVADA daquela
 *   ocorrência no dia; pendente ou sem pedido conta como não justificado
 * - diasAfastamento: dias cobertos por afastamento. Esses dias não têm
 *   previsto e nunca contam como falta. "afastamentos" lista os tipos
 *   (Férias, Atestado...) que apareceram no período
 * - diasEmAberto: dias que ainda não dá para fechar a conta (bateu a entrada
 *   e não a saída, ou é hoje e a pessoa ainda não bateu). Ficam fora do
 *   previsto e do realizado para não distorcer a diferença
 */
public record RelatorioIndicadoresDTO(
        long minutosPrevistos,
        long minutosRealizados,
        long diferencaMinutos,
        long minutosExtras,
        int atrasosJustificados,
        int atrasosNaoJustificados,
        long minutosAtraso,
        int faltasJustificadas,
        int faltasNaoJustificadas,
        int diasAfastamento,
        List<String> afastamentos,
        int diasEmAberto
) {
}
