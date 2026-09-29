package com.proyecto.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Edición de una oferta: solo cambian las cantidades; las ONGs participantes quedan fijas. */
public record OfertaEdicionRequest(
        @NotEmpty @Valid List<DetalleOfertaRequest> detalles
) {
}
