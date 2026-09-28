package com.proyecto.backend.dto;

import java.util.List;

public record AporteRecursoResponse(
        Long itemLoteId,
        String recursoNombre,
        String unidadMedida,
        Integer cantidadRequerida,
        Integer totalOfrecido,
        List<AporteOngResponse> porOng
) {
}
