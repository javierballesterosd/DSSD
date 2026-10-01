package com.proyecto.backend.dto;

import com.proyecto.backend.model.NivelGravedad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmergenciaRequest(
        @NotNull(message = "El nivel de gravedad es obligatorio")
        NivelGravedad nivelGravedad,

        @NotBlank(message = "La zona afectada es obligatoria")
        @Size(max = 200, message = "La zona no puede superar los 200 caracteres")
        String zonaAfectada,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 10, message = "La descripción debe tener al menos 10 caracteres")
        String descripcion
) {
}
