package com.mobdata.pontocerto.model;

// Ocorrência que uma JUSTIFICATIVA explica.
public enum TipoOcorrencia {

    ATRASO("Atraso"),
    FALTA("Falta"),
    SAIDA_ANTECIPADA("Saída antecipada");

    private final String rotulo;

    TipoOcorrencia(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
