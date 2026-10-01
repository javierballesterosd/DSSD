package com.proyecto.backend.service;

import com.proyecto.backend.dto.auth.LoginResponse;

/** Se publica al terminar de registrar una emergencia; la notificación se crea después del commit. */
public record EmergenciaRegistradaEvent(
        Long emergenciaId,
        String municipioNombre,
        String nivelGravedad,
        String zonaAfectada,
        String regionGroupPath,
        LoginResponse remitente
) {
}
