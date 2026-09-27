package com.proyecto.backend.dto;

import java.time.LocalDateTime;

public record EmergenciaResumenResponse(
        Long id,
        String zonaAfectada,
        String nivelGravedad,
        String nivelGravedadEtiqueta,
        String descripcion,
        LocalDateTime fechaRegistro,
        String municipio,
        LocalDateTime fechaCierreOfertas
) {
}
