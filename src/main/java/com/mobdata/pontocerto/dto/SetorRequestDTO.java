package com.mobdata.pontocerto.dto;

import com.mobdata.pontocerto.model.PoliticaForaPerimetro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record SetorRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Endereço é obrigatório")
        String endereco,

        @NotNull(message = "Latitude é obrigatória")
        Double latitude,

        @NotNull(message = "Longitude é obrigatória")
        Double longitude,

        @NotNull(message = "Raio é obrigatório")
        @Positive(message = "Raio deve ser maior que zero")
        Integer raioMetros,

        boolean exigirSelfie,

        @NotNull(message = "Política de fora do perímetro é obrigatória")
        PoliticaForaPerimetro politicaForaPerimetro,

        boolean ignorarLocalizacao,

        UUID empresaId
) {
}