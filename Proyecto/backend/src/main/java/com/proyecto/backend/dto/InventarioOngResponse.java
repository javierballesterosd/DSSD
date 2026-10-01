package com.proyecto.backend.dto;

import java.util.List;

public record InventarioOngResponse(
        Long ongId,
        String razonSocial,
        List<DisponibilidadRecursoResponse> recursos
) {
}
