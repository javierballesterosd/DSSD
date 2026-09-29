// EmergenciaRequestDTO.java
package com.proyecto.backend.dto.emergencia;


import com.proyecto.backend.model.NivelGravedad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmergenciaRequestDTO {

    @NotNull(message = "El nivel de gravedad es obligatorio")
    private NivelGravedad nivelGravedad;

    @NotBlank(message = "La zona afectada es obligatoria")
    @Size(max = 200, message = "La zona no puede superar los 255 caracteres")
    private String zonaAfectada;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 10, message = "La descripción debe tener al menos 10 caracteres")
    private String descripcion;
}