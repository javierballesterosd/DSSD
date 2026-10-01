package com.proyecto.backend.dto;

public record ItemLoteResponse(
        Long id,
        Long recursoId,
        String recursoNombre,
        String unidadMedida,
        Integer cantidadRequerida
) {
}
