package com.mobdata.pontocerto.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Saldo de banco de horas de um funcionário, congelado no momento em que o
 * mês da empresa foi fechado. "anoMes" existe só pra facilitar a busca do
 * fechamento anterior (ano*100+mes), ex.: outubro/2026 = 202610.
 */
@Entity
@Table(name = "tb_saldo_fechamento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_saldo_usuario_mes", columnNames = {"usuario_id", "ano_mes"})
})
@Getter
@Setter
@NoArgsConstructor
public class SaldoFechamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "ano_mes", nullable = false)
    private int anoMes;

    // Saldo acumulado total ao fim deste mês (já incluindo o que veio de meses anteriores).
    @Column(name = "saldo_acumulado_minutos", nullable = false)
    private long saldoAcumuladoMinutos;

    @ManyToOne
    @JoinColumn(name = "fechamento_id", nullable = false)
    private FechamentoPeriodo fechamento;
}