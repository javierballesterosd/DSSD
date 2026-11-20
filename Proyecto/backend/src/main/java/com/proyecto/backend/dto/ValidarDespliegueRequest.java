package com.proyecto.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidarDespliegueRequest(
        @NotBlank @Size(min = 10, max = 2000) String descripcionCierre) {}
