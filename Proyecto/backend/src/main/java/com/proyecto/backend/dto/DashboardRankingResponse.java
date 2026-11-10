package com.proyecto.backend.dto;

/** Total agregado por entidad; unidadMedida solo corresponde a recursos. */
public record DashboardRankingResponse(Long id, String nombre, long total, String unidadMedida) {
}
