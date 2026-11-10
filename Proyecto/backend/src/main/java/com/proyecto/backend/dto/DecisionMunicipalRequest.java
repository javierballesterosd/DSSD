package com.proyecto.backend.dto;

import jakarta.validation.constraints.NotNull;

public record DecisionMunicipalRequest(@NotNull Long loteId, @NotNull Boolean aceptar) {}
