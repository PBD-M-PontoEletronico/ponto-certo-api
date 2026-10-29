package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.TipoMarcacao;
import com.mobdata.pontocerto.model.TipoOcorrencia;
import com.mobdata.pontocerto.model.TipoSolicitacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * W10 — formulário multipart do pedido (campos + anexo opcional), no mesmo
 * estilo do AfastamentoForm.
 *
 * JUSTIFICATIVA: tipo, data, ocorrencia, motivo e (opcional) anexo.
 * AJUSTE:        tipo, data, tipoMarcacao, horaPretendida ("HH:mm") e motivo.
 *
 * usuarioId: o funcionário vai sem ele (é sempre o próprio). O RH usa para
 * registrar em nome de alguém da empresa. As regras por tipo ficam no service.
 */
@Getter
@Setter
public class SolicitacaoForm {

    private UUID usuarioId;

    @NotNull(message = "Tipo do pedido é obrigatório")
    private TipoSolicitacao tipo;

    @NotNull(message = "Data é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    private TipoOcorrencia ocorrencia;

    private TipoMarcacao tipoMarcacao;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaPretendida;

    @NotBlank(message = "Motivo é obrigatório")
    @Size(max = 1000, message = "Motivo deve ter no máximo 1000 caracteres")
    private String motivo;

    // Opcional (PDF, PNG ou JPG até 5 MB)
    private MultipartFile anexo;
}
