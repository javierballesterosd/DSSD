package com.proyecto.backend.service;

import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.model.EstadoLote;

import java.time.LocalDateTime;

/**
 * Se publica cuando cambia el lote de una emergencia (hoy, al publicarlo); la notificación al
 * municipio se crea después del commit. {@code estado} es el estado en que quedó el lote.
 */
public record LoteCambiadoEvent(
        Long loteId,
        String loteTitulo,
        EstadoLote estado,
        LocalDateTime fechaAperturaOfertas,
        LocalDateTime fechaCierreOfertas,
        Long emergenciaId,
        String zonaAfectada,
        String municipioGroupPath,
        LoginResponse remitente
) {
}
