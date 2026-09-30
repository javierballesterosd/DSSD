package com.proyecto.backend.dto.notificacion;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Representación pública de una notificación visible para el usuario autenticado.
 */
@Getter
@Builder
public class NotificacionResponseDTO {

    private Long id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaCreacion;
    private String remitenteUsername;
    private DescriptorAudiencia audiencia;
}
