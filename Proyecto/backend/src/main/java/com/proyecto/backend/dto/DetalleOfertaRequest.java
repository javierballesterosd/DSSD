package com.proyecto.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetalleOfertaRequest(
        @NotNull Long itemLoteId,
        @NotNull Long ongId,
        @NotNull @Min(1) Integer cantidadOfrecida
) {
}
