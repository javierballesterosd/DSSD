package com.proyecto.backend.dto.lote;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemLoteResponseDTO {

    private Long id;
    private Long recursoId;
    private String recursoNombre;
    private String unidadMedida;
    private Integer cantidadRequerida;
}