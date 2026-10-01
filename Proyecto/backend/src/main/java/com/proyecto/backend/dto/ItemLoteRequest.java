package com.proyecto.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemLoteRequest(
        @NotNull Long recursoId,
        @NotNull @Positive Integer cantidadRequerida
) {
}
