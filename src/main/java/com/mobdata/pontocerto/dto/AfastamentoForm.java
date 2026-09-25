package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.TipoAfastamento;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

// Formulário multipart (campos + arquivo opcional). Por isso é uma classe com
// setters, e não um record de JSON como os outros DTOs de entrada.
@Getter
@Setter
public class AfastamentoForm {

    @NotNull(message = "Funcionário é obrigatório")
    private UUID usuarioId;

    @NotNull(message = "Tipo é obrigatório")
    private TipoAfastamento tipo;

    @NotNull(message = "Data de início é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    // Opcional
    private MultipartFile anexo;
}
