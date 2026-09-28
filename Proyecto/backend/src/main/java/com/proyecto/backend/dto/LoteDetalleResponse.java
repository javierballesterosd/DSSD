package com.proyecto.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LoteDetalleResponse(
        Long id,
        String titulo,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaAperturaOfertas,
        LocalDateTime fechaCierreOfertas,
        EmergenciaResumenResponse emergencia,
        List<ItemLoteResponse> items,
        boolean convocatoriaAbierta
) {
}
