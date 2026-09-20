package com.mobdata.pontocerto.model;

public enum TipoAfastamento {

    FERIAS("Férias"),
    ATESTADO("Atestado"),
    LICENCA("Licença"),
    OUTRO("Outro");

    private final String rotulo;

    TipoAfastamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
