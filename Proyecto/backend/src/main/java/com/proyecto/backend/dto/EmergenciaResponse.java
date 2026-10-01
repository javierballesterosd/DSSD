package com.proyecto.backend.dto;

import com.proyecto.backend.model.NivelGravedad;

import java.time.LocalDateTime;

public record EmergenciaResponse(
        Long id,
        NivelGravedad nivelGravedad,
        String zonaAfectada,
        String descripcion,
        LocalDateTime fechaRegistro,
        String bonitaCaseId,
        Long municipioId
) {
}
