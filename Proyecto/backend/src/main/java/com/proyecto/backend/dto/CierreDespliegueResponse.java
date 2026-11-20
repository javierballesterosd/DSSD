package com.proyecto.backend.dto;

import java.time.LocalDateTime;

public record CierreDespliegueResponse(Long emergenciaId, String municipio, String zonaAfectada,
        String descripcion, long actividadesTotales, long actividadesPendientes,
        boolean adjudicada, boolean validacionRegistrada, boolean finalizada,
        String descripcionCierre, LocalDateTime fechaValidacion, LocalDateTime fechaFinalizacion) {}
