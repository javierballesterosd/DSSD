package com.proyecto.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record LoteRequest(
        @NotBlank String titulo,
        @NotNull LocalDateTime fechaAperturaOfertas,
        @NotNull LocalDateTime fechaCierreOfertas,
        @NotEmpty @Valid List<ItemLoteRequest> items
) {
}
