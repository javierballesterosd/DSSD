package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.lote.LoteRequestDTO;
import com.proyecto.backend.dto.lote.LoteResponseDTO;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.Lote;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LoteMapper {

    private final ItemLoteMapper itemLoteMapper;

    public LoteMapper(ItemLoteMapper itemLoteMapper) {
        this.itemLoteMapper = itemLoteMapper;
    }

    public Lote toEntity(
            LoteRequestDTO requestDTO,
            Emergencia emergencia
    ) {
        return Lote.builder()
                .titulo(requestDTO.getTitulo())
                .estado(EstadoLote.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .fechaInicio(requestDTO.getFechaInicio())
                .emergencia(emergencia)
                .build();
    }

    public LoteResponseDTO toDto(Lote lote) {
        return new LoteResponseDTO(
                lote.getId(),
                lote.getTitulo(),
                lote.getEstado(),
                lote.getFechaCreacion(),
                lote.getFechaInicio(),
                lote.getEmergencia().getId(),
                lote.getItems()
                        .stream()
                        .map(itemLoteMapper::toDto)
                        .toList()
        );
    }
}