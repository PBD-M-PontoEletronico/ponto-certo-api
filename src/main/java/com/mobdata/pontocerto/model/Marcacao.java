package com.mobdata.pontocerto.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma marcação de ponto do funcionário, como chegou do aparelho.
 *
 * O id NÃO é gerado aqui: vem do aparelho (o app gera um id único no toque).
 * Assim, se a mesma marcação offline for reenviada na sincronização, ela não
 * duplica — basta checar se o id já existe.
 *
 * Desvio do relógio: na abertura (GET /me/abertura) o app recebe a hora do
 * servidor e calcula
 *     desvioRelogioMs = horaDoAparelho - horaDoServidor
 * (positivo = celular adiantado; negativo = atrasado). Esse número viaja junto
 * com toda marcação feita offline. A hora real do toque é
 *     horaAparelho - desvioRelogioMs   (ver getHoraCorrigida()).
 */
@Entity
@Table(name = "tb_marcacao", indexes = {
        @Index(name = "idx_marcacao_usuario_hora", columnList = "usuario_id, hora_aparelho")
})
@Getter
@Setter
@NoArgsConstructor
public class Marcacao {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Setor em que a marcação foi feita (pode ser nulo se o aparelho não souber).
    @ManyToOne
    @JoinColumn(name = "setor_id")
    private Setor setor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMarcacao tipo;

    // Horário do toque segundo o relógio do celular (sem correção).
    @Column(name = "hora_aparelho", nullable = false)
    private Instant horaAparelho;

    // Diferença do relógio do celular em relação ao servidor, em milissegundos.
    // Nulo quando o aparelho ainda não tinha feito nenhuma abertura.
    @Column(name = "desvio_relogio_ms")
    private Long desvioRelogioMs;

    // true quando a marcação foi feita sem rede e só chegou depois.
    @Column(name = "offline", nullable = false)
    private boolean offline;

    // Quando o servidor recebeu a marcação.
    @Column(name = "recebida_em", nullable = false)
    private Instant recebidaEm;

    @ManyToOne
    @JoinColumn(name = "dispositivo_id")
    private Dispositivo dispositivo;

    // Quem estava logado no aparelho — igual a "usuario" no celular pessoal;
    // é a conta USUARIO_SETOR do tablet, no caso do relógio de setor.
    @ManyToOne
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_confirmacao")
    private MetodoConfirmacao metodoConfirmacao;

    // Confirmação (senha + foto/digital) acontece no aparelho, inclusive
    // offline — aqui só guardamos a evidência que sobrou (a foto, quando o
    // método foi FOTO). O arquivo fica em disco, igual ao Afastamento.
    @Column(name = "selfie_arquivo")
    private String selfieArquivo;

    @Column(name = "selfie_tipo_conteudo")
    private String selfieTipoConteudo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusMarcacao status;

    @Column(name = "motivo_recusa")
    private String motivoRecusa;

    // W10 — preenchido só nas marcações criadas pela aprovação de um pedido
    // de AJUSTE. Nulo = marcação batida no aparelho (a "original").
    // LAZY: as listagens só precisam do id, que o Hibernate já tem sem consulta extra.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id")
    private Solicitacao solicitacao;

    /** true quando esta marcação nasceu de um ajuste aprovado. */
    public boolean isAjuste() {
        return solicitacao != null;
    }

    /** Hora real do toque: a do aparelho descontado o desvio do relógio. */
    public Instant getHoraCorrigida() {
        if (horaAparelho == null) {
            return null;
        }
        return desvioRelogioMs == null ? horaAparelho : horaAparelho.minusMillis(desvioRelogioMs);
    }
}
