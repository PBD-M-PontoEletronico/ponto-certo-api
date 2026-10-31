package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.model.Marcacao;
import com.mobdata.pontocerto.model.StatusMarcacao;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Agrupa marcações válidas em jornadas (ENTRADA até a SAIDA correspondente,
 * descontando o intervalo quando houver) e atribui cada jornada ao dia da
 * ENTRADA — é isso que faz um turno que atravessa a meia-noite contar no dia
 * em que começou, e não no dia em que terminou.
 */
public final class HorasCalculo {

    private HorasCalculo() {
    }

    public record Jornada(LocalDate diaDeInicio, Instant horaEntrada, Integer minutosTrabalhados, boolean completa) {
    }

    public static List<Jornada> montarJornadas(List<Marcacao> marcacoesDoPeriodo, ZoneId fuso) {
        List<Marcacao> validas = marcacoesDoPeriodo.stream()
                .filter(m -> m.getStatus() == StatusMarcacao.VALIDA)
                .sorted(Comparator.comparing(Marcacao::getHoraCorrigida))
                .toList();

        List<Jornada> jornadas = new ArrayList<>();
        Marcacao entrada = null;
        Marcacao saidaIntervalo = null;
        Marcacao retornoIntervalo = null;

        for (Marcacao m : validas) {
            switch (m.getTipo()) {
                case ENTRADA -> {
                    if (entrada != null) {
                        jornadas.add(incompleta(entrada, fuso));
                    }
                    entrada = m;
                    saidaIntervalo = null;
                    retornoIntervalo = null;
                }
                case SAIDA_INTERVALO -> {
                    if (entrada != null) saidaIntervalo = m;
                }
                case RETORNO_INTERVALO -> {
                    if (entrada != null) retornoIntervalo = m;
                }
                case SAIDA -> {
                    if (entrada != null) {
                        jornadas.add(completa(entrada, saidaIntervalo, retornoIntervalo, m, fuso));
                        entrada = null;
                    }
                    // SAIDA sem ENTRADA correspondente: dado inconsistente, ignorado.
                }
            }
        }
        if (entrada != null) {
            jornadas.add(incompleta(entrada, fuso)); // dia ainda em andamento (ou faltou bater a saída)
        }

        return jornadas;
    }

    private static Jornada incompleta(Marcacao entrada, ZoneId fuso) {
        return new Jornada(entrada.getHoraCorrigida().atZone(fuso).toLocalDate(), entrada.getHoraCorrigida(), null, false);
    }

    private static Jornada completa(Marcacao entrada, Marcacao saidaIntervalo, Marcacao retornoIntervalo,
                                    Marcacao saida, ZoneId fuso) {
        long brutoMinutos = Duration.between(entrada.getHoraCorrigida(), saida.getHoraCorrigida()).toMinutes();

        long intervaloMinutos = 0;
        if (saidaIntervalo != null && retornoIntervalo != null
                && retornoIntervalo.getHoraCorrigida().isAfter(saidaIntervalo.getHoraCorrigida())) {
            intervaloMinutos = Duration.between(
                    saidaIntervalo.getHoraCorrigida(), retornoIntervalo.getHoraCorrigida()).toMinutes();
        }

        long liquido = Math.max(0, brutoMinutos - intervaloMinutos);
        return new Jornada(entrada.getHoraCorrigida().atZone(fuso).toLocalDate(), entrada.getHoraCorrigida(), (int) liquido, true);
    }
}