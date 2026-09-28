package com.proyecto.backend.dto;

public record DisponibilidadRecursoResponse(
        Long recursoId,
        String recursoNombre,
        String unidadMedida,
        Integer cantidadDisponible
) {
}
