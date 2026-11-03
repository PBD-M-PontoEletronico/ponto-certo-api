package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.dto.AgrupamentoRelatorio;
import com.mobdata.pontocerto.dto.EspelhoDiaDTO;
import com.mobdata.pontocerto.dto.EspelhoJustificativaDTO;
import com.mobdata.pontocerto.dto.RelatorioIndicadoresDTO;
import com.mobdata.pontocerto.dto.RelatorioLinhaDTO;
import com.mobdata.pontocerto.dto.RelatorioPeriodoDTO;
import com.mobdata.pontocerto.dto.SituacaoDia;
import com.mobdata.pontocerto.model.StatusMarcacao;
import com.mobdata.pontocerto.model.StatusSolicitacao;
import com.mobdata.pontocerto.model.TipoOcorrencia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * W12 — conta do previsto contra realizado, sem banco de dados: recebe os
 * dias já calculados pelo espelho (EspelhoService.diasDoPeriodo) e soma.
 * Fica separado do RelatorioService para dar para testar as regras sozinhas.
 *
 * Regras de cada dia (ver RelatorioIndicadoresDTO):
 * - dia em aberto (jornada sem saída, ou hoje sem nenhuma batida válida
 *   num dia de trabalho) não entra no previsto nem no realizado
 * - falta = dia de TRABALHO com previsto, já passado (antes de hoje), sem
 *   nenhuma marcação válida
 * - atraso = espelho calculou minutos de atraso > 0 no dia
 * - justificado = existe justificativa APROVADA da ocorrência no dia
 * - afastamento nunca vira falta (o espelho já zera o previsto desses dias)
 */
public final class RelatorioCalculo {

    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MM/yyyy");

    private RelatorioCalculo() {
    }

    // ---------------------------------------------------------------------
    // Períodos (colunas do relatório)
    // ---------------------------------------------------------------------

    /**
     * Quebra [inicio, fim] em semanas (segunda a domingo) ou meses, recortando
     * a primeira e a última fatia nos limites pedidos.
     */
    public static List<RelatorioPeriodoDTO> montarPeriodos(LocalDate inicio, LocalDate fim,
                                                           AgrupamentoRelatorio agrupamento) {
        List<RelatorioPeriodoDTO> periodos = new ArrayList<>();
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            LocalDate fimNatural = agrupamento == AgrupamentoRelatorio.SEMANA
                    ? cursor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    : YearMonth.from(cursor).atEndOfMonth();
            LocalDate fimFatia = fimNatural.isAfter(fim) ? fim : fimNatural;

            String rotulo;
            if (agrupamento == AgrupamentoRelatorio.MES) {
                rotulo = MES_ANO.format(cursor);
            } else if (cursor.equals(fimFatia)) {
                rotulo = DIA_MES.format(cursor); // semana recortada em um dia só
            } else {
                rotulo = DIA_MES.format(cursor) + " a " + DIA_MES.format(fimFatia);
            }

            periodos.add(new RelatorioPeriodoDTO(cursor, fimFatia, rotulo));
            cursor = fimFatia.plusDays(1);
        }
        return periodos;
    }

    // Posição do período que contém "data", ou -1 se estiver fora de todos.
    public static int indiceDoPeriodo(List<RelatorioPeriodoDTO> periodos, LocalDate data) {
        for (int i = 0; i < periodos.size(); i++) {
            RelatorioPeriodoDTO p = periodos.get(i);
            if (!data.isBefore(p.inicio()) && !data.isAfter(p.fim())) {
                return i;
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------------
    // Regras de um dia
    // ---------------------------------------------------------------------

    static boolean temMarcacaoValida(EspelhoDiaDTO dia) {
        return dia.marcacoes().stream().anyMatch(m -> m.status() == StatusMarcacao.VALIDA);
    }

    static boolean emAberto(EspelhoDiaDTO dia, LocalDate hoje) {
        if (dia.minutosTrabalhados() == null) {
            return true; // bateu entrada e não bateu saída
        }
        // Hoje, dia de trabalho, e ainda não bateu nada: o dia não acabou.
        return dia.data().equals(hoje)
                && dia.situacao() == SituacaoDia.TRABALHO
                && dia.minutosPrevistos() > 0
                && !temMarcacaoValida(dia);
    }

    static boolean ehFalta(EspelhoDiaDTO dia, LocalDate hoje) {
        return dia.situacao() == SituacaoDia.TRABALHO
                && dia.minutosPrevistos() > 0
                && dia.data().isBefore(hoje)
                && dia.minutosTrabalhados() != null
                && !temMarcacaoValida(dia);
    }

    static boolean justificado(EspelhoDiaDTO dia, TipoOcorrencia ocorrencia) {
        for (EspelhoJustificativaDTO j : dia.justificativas()) {
            if (j.ocorrencia() == ocorrencia && j.status() == StatusSolicitacao.APROVADA) {
                return true;
            }
        }
        return false;
    }

    /** Um dia conta para o relatório? (dias sem alocação e sem batida são ignorados) */
    public static boolean diaRelevante(EspelhoDiaDTO dia) {
        return dia.situacao() != SituacaoDia.SEM_ALOCACAO || !dia.marcacoes().isEmpty();
    }

    // ---------------------------------------------------------------------
    // Acumuladores
    // ---------------------------------------------------------------------

    /** Soma dos indicadores de um período (mutável, só usado durante a conta). */
    public static final class Acumulador {
        long minutosPrevistos;
        long minutosRealizados;
        long minutosExtras;
        int atrasosJustificados;
        int atrasosNaoJustificados;
        long minutosAtraso;
        int faltasJustificadas;
        int faltasNaoJustificadas;
        int diasAfastamento;
        final Set<String> afastamentos = new LinkedHashSet<>();
        int diasEmAberto;

        public void somarDia(EspelhoDiaDTO dia, LocalDate hoje) {
            if (dia.situacao() == SituacaoDia.AFASTAMENTO) {
                diasAfastamento++;
                if (dia.descricao() != null) {
                    afastamentos.add(dia.descricao());
                }
            }

            if (emAberto(dia, hoje)) {
                diasEmAberto++;
            } else {
                int previsto = dia.minutosPrevistos();
                int realizado = dia.minutosTrabalhados();
                minutosPrevistos += previsto;
                minutosRealizados += realizado;
                minutosExtras += Math.max(0, realizado - previsto);
            }

            if (ehFalta(dia, hoje)) {
                if (justificado(dia, TipoOcorrencia.FALTA)) faltasJustificadas++;
                else faltasNaoJustificadas++;
            }

            Integer atraso = dia.minutosAtraso();
            if (atraso != null && atraso > 0) {
                minutosAtraso += atraso;
                if (justificado(dia, TipoOcorrencia.ATRASO)) atrasosJustificados++;
                else atrasosNaoJustificados++;
            }
        }

        public RelatorioIndicadoresDTO paraDTO() {
            return new RelatorioIndicadoresDTO(
                    minutosPrevistos,
                    minutosRealizados,
                    minutosRealizados - minutosPrevistos,
                    minutosExtras,
                    atrasosJustificados,
                    atrasosNaoJustificados,
                    minutosAtraso,
                    faltasJustificadas,
                    faltasNaoJustificadas,
                    diasAfastamento,
                    List.copyOf(afastamentos),
                    diasEmAberto
            );
        }
    }

    /** Uma linha do relatório: total + um acumulador por período. */
    public static final class Linha {
        private final UUID id;
        private final String nome;
        private final String detalhe;
        private final List<RelatorioPeriodoDTO> periodos;
        private final Acumulador total = new Acumulador();
        private final Acumulador[] porPeriodo;
        private int diasSomados;

        public Linha(UUID id, String nome, String detalhe, List<RelatorioPeriodoDTO> periodos) {
            this.id = id;
            this.nome = nome;
            this.detalhe = detalhe;
            this.periodos = periodos;
            this.porPeriodo = new Acumulador[periodos.size()];
            for (int i = 0; i < porPeriodo.length; i++) {
                porPeriodo[i] = new Acumulador();
            }
        }

        public void somarDia(EspelhoDiaDTO dia, LocalDate hoje) {
            int indice = indiceDoPeriodo(periodos, dia.data());
            if (indice < 0) {
                return;
            }
            total.somarDia(dia, hoje);
            porPeriodo[indice].somarDia(dia, hoje);
            diasSomados++;
        }

        public boolean vazia() {
            return diasSomados == 0;
        }

        public String nome() {
            return nome;
        }

        public UUID id() {
            return id;
        }

        public RelatorioLinhaDTO paraDTO() {
            List<RelatorioIndicadoresDTO> colunas = new ArrayList<>();
            for (Acumulador a : porPeriodo) {
                colunas.add(a.paraDTO());
            }
            return new RelatorioLinhaDTO(id, nome, detalhe, total.paraDTO(), colunas);
        }
    }
}
