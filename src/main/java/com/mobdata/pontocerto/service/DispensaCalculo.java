package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.model.Afastamento;
import com.mobdata.pontocerto.model.Feriado;
import com.mobdata.pontocerto.model.Setor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Regras puras de "dia dispensado": feriado e afastamento não geram turno
 * esperado, portanto não geram falta nem atraso. Não depende de Spring nem
 * de banco, para poder ser testada isoladamente (mesmo estilo do EscalaCalculo).
 */
public final class DispensaCalculo {

    private DispensaCalculo() {
    }

    /** Afastamento do funcionário que cobre o dia (início e fim inclusive). */
    public static Optional<Afastamento> afastamentoNoDia(List<Afastamento> afastamentos, LocalDate data) {
        return afastamentos.stream()
                .filter(a -> !data.isBefore(a.getDataInicio()) && !data.isAfter(a.getDataFim()))
                .findFirst();
    }

    /**
     * Feriado que vale para quem está naquele setor no dia: o da empresa
     * inteira (setor nulo) ou o do próprio setor. Feriado de outro setor
     * não conta.
     */
    public static Optional<Feriado> feriadoNoDia(List<Feriado> feriados, Setor setor, LocalDate data) {
        return feriados.stream()
                .filter(f -> f.getData().equals(data))
                .filter(f -> f.getSetor() == null || f.getSetor().getId().equals(setor.getId()))
                .findFirst();
    }

    public static boolean dispensado(List<Afastamento> afastamentos, List<Feriado> feriados,
                                     Setor setor, LocalDate data) {
        return afastamentoNoDia(afastamentos, data).isPresent()
                || feriadoNoDia(feriados, setor, data).isPresent();
    }
}
