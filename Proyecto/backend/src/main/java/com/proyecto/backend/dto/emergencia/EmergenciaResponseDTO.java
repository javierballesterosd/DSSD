package com.proyecto.backend.dto.emergencia;

import com.proyecto.backend.model.NivelGravedad;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmergenciaResponseDTO {

    private Long id;
    private NivelGravedad nivelGravedad;
    private String zonaAfectada;
    private String descripcion;
    private LocalDateTime fechaRegistro;
    private String bonitaCaseId;
    private Long municipioId;
}