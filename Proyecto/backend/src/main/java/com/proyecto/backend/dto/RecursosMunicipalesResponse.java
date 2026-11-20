package com.proyecto.backend.dto;

import java.util.List;

/** Cantidades habilitadas por el SN, con cuánto aporta cada ONG. */
public record RecursosMunicipalesResponse(Long loteId, Boolean decision, boolean confirmada,
        boolean puedeDecidir, String mensaje, List<RecursoAceptado> recursos) {
    // cantidadRequerida: lo que pedía el lote de ese recurso; cantidad: lo que habilitó el SN
    public record RecursoAceptado(Long recursoId, String nombre, String unidadMedida, long cantidadRequerida,
            long cantidad, List<AporteOng> porOng) {}
    public record AporteOng(Long ongId, String razonSocial, long cantidad) {}
}
