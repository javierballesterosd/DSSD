package com.proyecto.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record OfertaRequest(
        @NotNull Long loteId,
        @NotEmpty Set<Long> ongIds,
        @NotEmpty @Valid List<DetalleOfertaRequest> detalles
) {
}
