package com.proyecto.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LoteResumenResponse(
        Long id,
        String titulo,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDate fechaInicio,
        EmergenciaResumenResponse emergencia,
        List<ItemLoteResponse> items
) {
}
