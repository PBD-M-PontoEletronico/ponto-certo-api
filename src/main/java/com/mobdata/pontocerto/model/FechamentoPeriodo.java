package com.mobdata.pontocerto.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Mês fechado de uma empresa. Versão mínima, criada na W10 só para que
 * pedidos de justificativa/ajuste de um mês já fechado sejam recusados.
 * O card de fechamento (E3) pode crescer em cima desta tabela.
 *
 * Existir uma linha = o mês está fechado. Reabrir = apagar a linha.
 */
@Entity
@Table(name = "tb_fechamento_periodo", uniqueConstraints = {
        @UniqueConstraint(name = "uk_fechamento_empresa_mes", columnNames = {"empresa_id", "ano", "mes"})
})
@Getter
@Setter
@NoArgsConstructor
public class FechamentoPeriodo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "ano", nullable = false)
    private int ano;

    @Column(name = "mes", nullable = false)
    private int mes;

    @Column(name = "fechado_em", nullable = false)
    private Instant fechadoEm;

    @ManyToOne
    @JoinColumn(name = "fechado_por_id", nullable = false)
    private Usuario fechadoPor;
}
