package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.MetodoConfirmacao;
import com.mobdata.pontocerto.model.TipoMarcacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Formulário multipart de POST /marcacoes (mesmo padrão do AfastamentoForm).
 *
 * O "id" vem do aparelho (gerado no toque, igual já funciona em Marcacao.java)
 * — é a chave de idempotência: se a sincronização reenviar a mesma marcação,
 * o servidor detecta pelo id e não duplica nem revalida nada.
 *
 * Não existe campo de senha aqui: a confirmação (senha + foto OU senha +
 * digital) acontece no aparelho, no momento do toque — inclusive offline. O
 * servidor não teria como revalidar isso depois, na sincronização. O que
 * chega aqui é só a evidência (a foto, ou a garantia de que o SO confirmou a
 * biometria) — ver MarcacaoService para os detalhes do que É validado aqui.
 */
@Getter
@Setter
public class MarcacaoForm {

    @NotNull(message = "Id é obrigatório")
    private UUID id;

    @NotNull(message = "Usuário é obrigatório")
    private UUID usuarioId;

    // Opcional — o aparelho pode não saber o setor (ex: localização desligada)
    private UUID setorId;

    @NotNull(message = "Tipo é obrigatório")
    private TipoMarcacao tipo;

    // Hora do relógio do aparelho, em epoch millis — mesmo formato de
    // horaServidorEpochMs que a abertura (GET /me/abertura) já manda pro app.
    @NotNull(message = "Hora do aparelho é obrigatória")
    private Long horaAparelhoEpochMs;

    // Diferença (aparelho - servidor) calculada na última abertura; nulo se o
    // app nunca abriu com rede antes desta marcação.
    private Long desvioRelogioMs;

    private boolean offline;

    private Double latitude;

    private Double longitude;

    @NotBlank(message = "Identificador do dispositivo é obrigatório")
    private String identificadorDispositivo;

    @NotNull(message = "Método de confirmação é obrigatório")
    private MetodoConfirmacao metodoConfirmacao;

    // Só usado quando metodoConfirmacao == DIGITAL — o app afirma que o SO
    // confirmou a biometria localmente antes do toque ser salvo.
    private Boolean digitalConfirmada;

    // Só usada quando metodoConfirmacao == FOTO
    private MultipartFile foto;
}