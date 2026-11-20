package com.proyecto.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Actividad de la emergencia. {@code finalizada} es derivada: todas las ofertas adjudicadas la completaron.
 * {@code avance} detalla cada oferta (con las ONGs que la integran) para el seguimiento del CCR.
 */
public record ActividadResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean finalizada,
        int totalOfertas,
        int ofertasFinalizadas,
        List<AvanceOferta> avance
) {
    public record AvanceOferta(
            Long ofertaId,
            List<String> ongs,
            boolean finalizada,
            LocalDateTime fechaFinalizacion,
            String finalizadaPor
    ) {
    }
}
