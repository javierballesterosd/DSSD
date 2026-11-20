package com.proyecto.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Oferta adjudicada de la ONG con el avance de sus actividades (pantalla del representante de ONG). */
public record MiDespliegueResponse(
        Long ofertaId,
        String estado,
        String estadoEtiqueta,
        Long emergenciaId,
        String emergenciaZona,
        String nivelGravedadEtiqueta,
        String municipio,
        Long loteId,
        String loteTitulo,
        /** Razón social de todas las ONGs de la oferta (más de una implica consorcio). */
        List<String> ongs,
        int totalActividades,
        int actividadesFinalizadas,
        List<Actividad> actividades
) {
    public record Actividad(
            Long actividadId,
            String nombre,
            String descripcion,
            boolean finalizada,
            LocalDateTime fechaFinalizacion,
            /** ONG desde la que se marcó como hecha. */
            String finalizadaPor,
            String usuario
    ) {
    }
}
