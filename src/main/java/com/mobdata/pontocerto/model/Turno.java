package com.mobdata.pontocerto.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "turno")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "escala_id", nullable = false)
    @JsonIgnore
    private Escala escala;

    private LocalTime horaInicio;

    private LocalTime horaFim;

    private int intervaloMinutos;

    public Turno() {
    }

    public Turno(LocalTime horaInicio, LocalTime horaFim, int intervaloMinutos) {
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.intervaloMinutos = intervaloMinutos;
    }

    public boolean atravessaMeiaNoite() {
        return !horaFim.isAfter(horaInicio);
    }

    public int duracaoMinutos() {
        int inicio = horaInicio.toSecondOfDay() / 60;
        int fim = horaFim.toSecondOfDay() / 60;
        if (fim <= inicio) {
            fim += 24 * 60;
        }
        return fim - inicio - intervaloMinutos;
    }

    public UUID getId() {
        return id;
    }

    public Escala getEscala() {
        return escala;
    }

    public void setEscala(Escala escala) {
        this.escala = escala;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime horaFim) {
        this.horaFim = horaFim;
    }

    public int getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public void setIntervaloMinutos(int intervaloMinutos) {
        this.intervaloMinutos = intervaloMinutos;
    }
}