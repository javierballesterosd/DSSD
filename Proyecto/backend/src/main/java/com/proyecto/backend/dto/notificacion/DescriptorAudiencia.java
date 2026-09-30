package com.proyecto.backend.dto.notificacion;

/**
 * Identifica de forma explícita el público destinatario de una notificación.
 *
 * @param grupoDestinatario path del grupo padre de Bonita
 */
public record DescriptorAudiencia(
        String grupoDestinatario
) {
}
