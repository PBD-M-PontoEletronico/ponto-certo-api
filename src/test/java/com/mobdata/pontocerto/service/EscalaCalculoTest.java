package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.model.Escala;
import com.mobdata.pontocerto.model.ModelEscala;
import com.mobdata.pontocerto.model.Turno;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EscalaCalculoTest {

    private Escala escalaComTurno(ModelEscala modelo, LocalDate dataReferencia, LocalTime inicio, LocalTime fim) {
        Escala escala = new Escala();
        escala.setModelo(modelo);
        escala.setDataReferencia(dataReferencia);
        escala.setTurnos(List.of(new Turno(inicio, fim, 0)));
        return escala;
    }

    @Test
    void turnoQueAtravessaMeiaNoiteGeraPeriodoNoDiaSeguinte() {
        // 12x36: entra 19h, sai 7h do dia seguinte
        Escala escala = escalaComTurno(ModelEscala.JORNADA_12X36, LocalDate.of(2026, 1, 1),
                LocalTime.of(19, 0), LocalTime.of(7, 0));

        List<EscalaCalculo.PeriodoTurno> periodos =
                EscalaCalculo.projetarTurnos(escala, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertThat(periodos).hasSize(1);
        EscalaCalculo.PeriodoTurno periodo = periodos.get(0);
        assertThat(periodo.inicio()).isEqualTo(LocalDateTime.of(2026, 1, 1, 19, 0));
        assertThat(periodo.fim()).isEqualTo(LocalDateTime.of(2026, 1, 2, 7, 0));
    }

    @Test
    void cicloDozeTrintaESeisAlternaDiaSimDiaNao() {
        Escala escala = escalaComTurno(ModelEscala.JORNADA_12X36, LocalDate.of(2026, 1, 1),
                LocalTime.of(7, 0), LocalTime.of(19, 0));

        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 1))).isTrue();
        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 2))).isFalse();
        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 3))).isTrue();
    }

    @Test
    void cicloVinteEQuatroPorSetentaEDoisTrabalhaUmDiaACadaQuatro() {
        Escala escala = escalaComTurno(ModelEscala.JORNADA_24X72, LocalDate.of(2026, 1, 1),
                LocalTime.of(7, 0), LocalTime.of(7, 0));

        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 1))).isTrue();
        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 2))).isFalse();
        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 3))).isFalse();
        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 4))).isFalse();
        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 5))).isTrue();
    }

    @Test
    void comercialCincoPorDoisNaoTrabalhaNoFimDeSemana() {
        Escala escala = escalaComTurno(ModelEscala.COMERCIAL_5X2, null, LocalTime.of(8, 0), LocalTime.of(18, 0));

        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 5))).isTrue(); // segunda
        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 10))).isFalse(); // sábado
        assertThat(EscalaCalculo.ehDiaDeTrabalho(escala, LocalDate.of(2026, 1, 11))).isFalse(); // domingo
    }

    @Test
    void projecaoDeMultiplosDiasSoIncluiDiasDeTrabalho() {
        Escala escala = escalaComTurno(ModelEscala.COMERCIAL_5X2, null, LocalTime.of(8, 0), LocalTime.of(18, 0));

        List<EscalaCalculo.PeriodoTurno> periodos = EscalaCalculo.projetarTurnos(
                escala, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11)); // seg a dom

        assertThat(periodos).hasSize(5); // seg a sex, sem sábado/domingo
    }
}
