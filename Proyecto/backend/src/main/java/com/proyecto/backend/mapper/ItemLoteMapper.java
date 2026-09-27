package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.lote.ItemLoteRequestDTO;
import com.proyecto.backend.dto.lote.ItemLoteResponseDTO;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.Recurso;
import org.springframework.stereotype.Component;

@Component
public class ItemLoteMapper {

    public ItemLote toEntity(
            ItemLoteRequestDTO requestDTO,
            Recurso recurso
    ) {
        return ItemLote.builder()
                .cantidadRequerida(requestDTO.getCantidadRequerida())
                .recurso(recurso)
                .build();
    }

    public ItemLoteResponseDTO toDto(ItemLote item) {
        return new ItemLoteResponseDTO(
                item.getId(),
                item.getRecurso().getId(),
                item.getRecurso().getNombre(),
                item.getRecurso().getUnidadMedida(),
                item.getCantidadRequerida()
        );
    }
}