package com.proyecto.backend.dto.lote;

import com.proyecto.backend.model.EstadoLote;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class LoteResponse {

    private Long id;
    private String titulo;
    private EstadoLote estado;
    private LocalDateTime fechaCreacion;
    private LocalDate fechaInicio;
    private Long emergenciaId;
    private List<ItemLoteResponse> items;
}