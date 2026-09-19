package com.proyecto.backend.model;

public enum NivelGravedad {
    BAJA("Baja", "Afecta a pocas familias, sin riesgo para la vida"),
    MEDIA("Media", "Afecta a varios barrios; hay daños materiales y riesgo limitado para las personas"),
    ALTA("Alta", "Afecta a gran parte del municipio; hay personas en riesgo y servicios básicos interrumpidos"),
    CRITICA("Crítica", "Riesgo de vida inmediato para muchas personas; requiere respuesta regional urgente");

    private final String etiqueta;
    private final String descripcion;

    NivelGravedad(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
