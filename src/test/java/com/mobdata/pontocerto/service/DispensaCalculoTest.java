package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.model.Afastamento;
import com.mobdata.pontocerto.model.Feriado;
import com.mobdata.pontocerto.model.Setor;
import com.mobdata.pontocerto.model.TipoAfastamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DispensaCalculoTest {

    private Setor setorA;
    private Setor setorB;

    @BeforeEach
    void setUp() {
        setorA = new Setor();
        setorA.setId(UUID.randomUUID());
        setorB = new Setor();
        setorB.setId(UUID.randomUUID());
    }

    private Feriado feriado(LocalDate data, Setor setor) {
        Feriado feriado = new Feriado();
        feriado.setData(data);
        feriado.setSetor(setor);
        feriado.setDescricao("Feriado de teste");
        return feriado;
    }

    private Afastamento afastamento(LocalDate inicio, LocalDate fim) {
        Afastamento afastamento = new Afastamento();
        afastamento.setTipo(TipoAfastamento.FERIAS);
        afastamento.setDataInicio(inicio);
        afastamento.setDataFim(fim);
        return afastamento;
    }

    @Test
    void feriadoDaEmpresaInteiraValeParaQualquerSetor() {
        LocalDate dia = LocalDate.of(2026, 9, 7);
        List<Feriado> feriados = List.of(feriado(dia, null));

        assertThat(DispensaCalculo.feriadoNoDia(feriados, setorA, dia)).isPresent();
        assertThat(DispensaCalculo.feriadoNoDia(feriados, setorB, dia)).isPresent();
    }

    @Test
    void feriadoDeUmSetorNaoValeParaOutroSetor() {
        LocalDate dia = LocalDate.of(2026, 9, 7);
        List<Feriado> feriados = List.of(feriado(dia, setorA));

        assertThat(DispensaCalculo.feriadoNoDia(feriados, setorA, dia)).isPresent();
        assertThat(DispensaCalculo.feriadoNoDia(feriados, setorB, dia)).isEmpty();
    }

    @Test
    void feriadoSoValeNaPropriaData() {
        List<Feriado> feriados = List.of(feriado(LocalDate.of(2026, 9, 7), null));

        assertThat(DispensaCalculo.feriadoNoDia(feriados, setorA, LocalDate.of(2026, 9, 8))).isEmpty();
    }

    @Test
    void afastamentoCobreOsDiasDeInicioEFimInclusive() {
        List<Afastamento> afastamentos = List.of(
                afastamento(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9)));

        assertThat(DispensaCalculo.afastamentoNoDia(afastamentos, LocalDate.of(2026, 10, 4))).isEmpty();
        assertThat(DispensaCalculo.afastamentoNoDia(afastamentos, LocalDate.of(2026, 10, 5))).isPresent();
        assertThat(DispensaCalculo.afastamentoNoDia(afastamentos, LocalDate.of(2026, 10, 9))).isPresent();
        assertThat(DispensaCalculo.afastamentoNoDia(afastamentos, LocalDate.of(2026, 10, 10))).isEmpty();
    }

    @Test
    void diaDispensadoQuandoTemFeriadoOuAfastamento() {
        LocalDate diaFeriado = LocalDate.of(2026, 11, 2);
        LocalDate diaAfastado = LocalDate.of(2026, 11, 10);
        LocalDate diaNormal = LocalDate.of(2026, 11, 11);

        List<Feriado> feriados = List.of(feriado(diaFeriado, null));
        List<Afastamento> afastamentos = List.of(afastamento(diaAfastado, diaAfastado));

        assertThat(DispensaCalculo.dispensado(afastamentos, feriados, setorA, diaFeriado)).isTrue();
        assertThat(DispensaCalculo.dispensado(afastamentos, feriados, setorA, diaAfastado)).isTrue();
        assertThat(DispensaCalculo.dispensado(afastamentos, feriados, setorA, diaNormal)).isFalse();
    }
}
