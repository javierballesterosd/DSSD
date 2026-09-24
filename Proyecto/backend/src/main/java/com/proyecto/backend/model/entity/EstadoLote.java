package com.proyecto.backend.model.entity;

public enum EstadoLote {
    ACTIVO("Activo", "Lote publicado y abierto a ofertas o en ejecución"),
    FINALIZADO("Finalizado", "Las actividades del lote se completaron y sus recursos fueron liberados"),
    CANCELADO("Cancelado", "El lote fue dado de baja y ya no necesita cobertura");

    private final String etiqueta;
    private final String descripcion;

    EstadoLote(String etiqueta, String descripcion) {
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
