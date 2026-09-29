package com.proyecto.backend.model;

public enum EstadoOferta {
    PENDIENTE("Pendiente", "Cargada por la ONG, a la espera de la validación del Sistema Nacional"),
    ACEPTADA_PARCIAL("Aceptada parcialmente", "El Sistema Nacional habilitó solo una parte de lo ofrecido"),
    VALIDADA("Validada", "Habilitada por el Sistema Nacional; el municipio puede elegirla"),
    ADJUDICADA("Adjudicada", "Elegida por el municipio; los recursos quedan comprometidos"),
    NO_ADJUDICADA("No adjudicada", "Validada pero no elegida por el municipio"),
    EN_EJECUCION("En ejecución", "La ONG está desplegando los recursos adjudicados"),
    FINALIZADA("Finalizada", "La ONG completó todas sus actividades"),
    RECHAZADA("Rechazada", "El Sistema Nacional no habilitó la oferta"),
    ELIMINADA("Eliminada", "Dada de baja por la ONG dentro de la ventana de ofertas");

    private final String etiqueta;
    private final String descripcion;

    EstadoOferta(String etiqueta, String descripcion) {
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
