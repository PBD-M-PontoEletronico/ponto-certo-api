package com.mobdata.pontocerto.model;
//Entidade para auditoria de fechar/abrir o mês

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_fechamento_log")
@Getter
@Setter
@NoArgsConstructor
public class FechamentoLog {
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

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, length = 20)
    private AcaoFechamento acao;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "quando", nullable = false)
    private Instant quando;
}
