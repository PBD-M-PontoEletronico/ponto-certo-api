package com.mobdata.pontocerto.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "escala")
public class Escala {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    private String nome;

    @Enumerated(EnumType.STRING)
    private ModelEscala modelo;

    @OneToMany(mappedBy = "escala", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Turno> turnos = new ArrayList<>();

    private LocalDate dataReferencia;

    @ElementCollection
    @CollectionTable(name = "escala_dia_semana", joinColumns = @JoinColumn(name = "escala_id"))
    private Set<Integer> diasSemana = new HashSet<>();

    public Escala() {
    }

    public UUID getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public ModelEscala getModelo() {
        return modelo;
    }

    public void setModelo(ModelEscala modelo) {
        this.modelo = modelo;
    }

    public List<Turno> getTurnos() {
        return turnos;
    }

    public void setTurnos(List<Turno> novosTurnos) {
        turnos.clear();
        for (Turno turno : novosTurnos) {
            turno.setEscala(this);
            turnos.add(turno);
        }
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public void setDataReferencia(LocalDate dataReferencia) {
        this.dataReferencia = dataReferencia;
    }

    public Set<Integer> getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(Set<Integer> diasSemana) {
        this.diasSemana = diasSemana;
    }
}