package com.proyecto.backend.dto.lote;

import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.NivelGravedad;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EmergenciaLoteResponseDTO {

    private Long id;
    private String descripcion;
    private NivelGravedad nivelGravedad;
    private String zonaAfectada;
    private Long municipioId;

    private LocalDateTime fechaRegistro;
    private Long loteId;
    private EstadoLote estadoLote;
}