package com.mobdata.pontocerto.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_feriado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Feriado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // Nulo = feriado da empresa inteira. Preenchido = vale só para esse setor.
    // ON DELETE CASCADE: excluir o setor leva junto os feriados dele, sem
    // travar a exclusão (feriado não é um "vínculo" que deva impedir isso).
    @ManyToOne
    @JoinColumn(name = "setor_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Setor setor;

    @Column(name = "data_feriado", nullable = false)
    private LocalDate data;

    @Column(name = "descricao", nullable = false)
    private String descricao;
}
