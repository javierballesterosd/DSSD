package com.proyecto.backend.dto;


public final class DashboardBonitaResponse {
    private DashboardBonitaResponse() {}
    public record Tiempo(String etapa, long cantidad, Double promedioSegundos) {}
    public record EmergenciaLocal(Long emergenciaId, String caseId, String municipio, String descripcion) {}
    public record Seguimiento(Long emergenciaId, String caseId, String municipio, String descripcion,
                              String etapa, String estado) {}
}
