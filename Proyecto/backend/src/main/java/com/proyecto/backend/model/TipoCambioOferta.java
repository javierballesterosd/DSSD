package com.proyecto.backend.model;

public enum TipoCambioOferta {
    CREACION("Registro"),
    EDICION("Edición"),
    BAJA("Baja");

    private final String etiqueta;

    TipoCambioOferta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
