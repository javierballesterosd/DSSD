package com.proyecto.backend.dto;

import java.time.LocalDateTime;

/** Representación pública de una notificación visible para el usuario autenticado. */
public record NotificacionResponse(
        Long id,
        String titulo,
        String descripcion,
        LocalDateTime fechaCreacion,
        String remitenteUsername,
        DescriptorAudiencia audiencia
) {
}
