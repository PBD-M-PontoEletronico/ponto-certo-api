package com.mobdata.pontocerto.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * W10 — pedido de justificativa ou de ajuste de ponto.
 *
 * Todo pedido guarda quem pediu (solicitante) e quando (criadoEm), e, depois
 * da análise, quem analisou (analisadoPor), quando (analisadoEm) e o parecer
 * escrito. O solicitante normalmente é o próprio funcionário (app), mas pode
 * ser o RH registrando em nome dele pela web — por isso são dois campos.
 */
@Entity
@Table(name = "tb_solicitacao", indexes = {
        @Index(name = "idx_solicitacao_usuario_data", columnList = "usuario_id, data"),
        @Index(name = "idx_solicitacao_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Impede que duas pessoas aprovem o mesmo pedido ao mesmo tempo (o que
    // geraria duas marcações de ajuste). Quem salvar por último recebe erro.
    @Version
    @Column(name = "versao")
    private Long versao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoSolicitacao tipo;

    // Funcionário a quem o pedido se refere.
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Quem registrou o pedido (o próprio funcionário ou o RH em nome dele).
    @ManyToOne
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    // Dia a que o pedido se refere (dia da ocorrência ou da marcação esquecida).
    @Column(name = "data", nullable = false)
    private LocalDate data;

    // Só JUSTIFICATIVA
    @Enumerated(EnumType.STRING)
    @Column(name = "ocorrencia")
    private TipoOcorrencia ocorrencia;

    // Só AJUSTE: qual marcação faltou e em que horário devia ter sido.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_marcacao")
    private TipoMarcacao tipoMarcacao;

    @Column(name = "hora_pretendida")
    private LocalTime horaPretendida;

    @Column(name = "motivo", nullable = false, length = 1000)
    private String motivo;

    // Anexo opcional (atestado...). Mesmo esquema do Afastamento: arquivo em
    // disco, aqui só o nome salvo, o nome original e o tipo.
    @Column(name = "anexo_arquivo")
    private String anexoArquivo;

    @Column(name = "anexo_nome_original")
    private String anexoNomeOriginal;

    @Column(name = "anexo_tipo_conteudo")
    private String anexoTipoConteudo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusSolicitacao status;

    @Column(name = "parecer", length = 1000)
    private String parecer;

    @ManyToOne
    @JoinColumn(name = "analisado_por_id")
    private Usuario analisadoPor;

    @Column(name = "analisado_em")
    private Instant analisadoEm;

    // Marcação criada quando um AJUSTE é aprovado.
    @OneToOne
    @JoinColumn(name = "marcacao_gerada_id")
    private Marcacao marcacaoGerada;
}
