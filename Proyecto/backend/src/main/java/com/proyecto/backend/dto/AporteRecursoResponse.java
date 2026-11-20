package com.proyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record AporteRecursoResponse(
        Long itemLoteId,
        String recursoNombre,
        String unidadMedida,
        Integer cantidadRequerida,
        Integer totalOfrecido,
        // Suma de lo habilitado por el SN; null si la oferta todavía no fue validada
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer totalAceptado,
        List<AporteOngResponse> porOng
) {
}
