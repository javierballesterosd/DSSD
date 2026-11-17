package com.proyecto.backend.dto;

public record ActividadResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean finalizada
) {
}