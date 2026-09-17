package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.model.ModelEscala;
import com.mobdata.pontocerto.model.Turno;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras puras de projeção de escala: em que dias ela prevê trabalho e, a
 * partir disso, os períodos concretos (com data e hora) que cada turno ocupa.
 * Não depende de Spring nem de banco, para poder ser testada isoladamente.
 */
public final class EscalaCalculo {

    private EscalaCalculo() {
    }

    public static boolean ehDiaDeTrabalho(Escala escala, LocalDate data) {
        Integer tamanhoCiclo = tamanhoDoCiclo(escala.getModelo());

        if (tamanhoCiclo != null) {
            if (escala.getDataReferencia() == null) {
                return false;
            }
            long diferencaDias = ChronoUnit.DAYS.between(escala.getDataReferencia(), data);
            long posicao = Math.floorMod(diferencaDias, tamanhoCiclo);
            return posicao == 0;
        }

        var diasSemana = escala.getDiasSemana();

        if (escala.getModelo() == ModelEscala.COMERCIAL_5X2) {
            if (diasSemana != null && !diasSemana.isEmpty()) {
                return diasSemana.contains(data.getDayOfWeek().getValue() % 7);
            }
            int diaSemana = data.getDayOfWeek().getValue();
            return diaSemana >= 1 && diaSemana <= 5;
        }

        if (diasSemana != null && !diasSemana.isEmpty()) {
            return diasSemana.contains(data.getDayOfWeek().getValue() % 7);
        }

        return true;
    }

    /**
     * Projeta os turnos da escala em [inicio, fim] (inclusive) como períodos
     * concretos de data e hora. Turno que atravessa a meia-noite gera um
     * período cujo fim cai no dia seguinte ao início.
     */
    public static List<PeriodoTurno> projetarTurnos(Escala escala, LocalDate inicio, LocalDate fim) {
        List<PeriodoTurno> periodos = new ArrayList<>();

        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            if (!ehDiaDeTrabalho(escala, data)) {
                continue;
            }
            for (Turno turno : escala.getTurnos()) {
                LocalDateTime inicioTurno = LocalDateTime.of(data, turno.getHoraInicio());
                LocalDateTime fimTurno = LocalDateTime.of(data, turno.getHoraFim());
                if (turno.atravessaMeiaNoite()) {
                    fimTurno = fimTurno.plusDays(1);
                }
                periodos.add(new PeriodoTurno(inicioTurno, fimTurno));
            }
        }

        return periodos;
    }

    private static Integer tamanhoDoCiclo(ModelEscala modelo) {
        if (modelo == ModelEscala.JORNADA_24X72) return 4;
        if (modelo == ModelEscala.JORNADA_12X36) return 2;
        return null;
    }

    public record PeriodoTurno(LocalDateTime inicio, LocalDateTime fim) {
    }
}
