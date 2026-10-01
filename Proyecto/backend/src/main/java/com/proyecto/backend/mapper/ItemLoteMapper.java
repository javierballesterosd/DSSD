package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.ItemLoteRequest;
import com.proyecto.backend.dto.ItemLoteResponse;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.Recurso;
import org.springframework.stereotype.Component;

@Component
public class ItemLoteMapper {

    public ItemLote toEntity(ItemLoteRequest request, Recurso recurso) {
        return ItemLote.builder()
                .cantidadRequerida(request.cantidadRequerida())
                .recurso(recurso)
                .build();
    }

    public ItemLoteResponse toResponse(ItemLote item) {
        return new ItemLoteResponse(
                item.getId(),
                item.getRecurso().getId(),
                item.getRecurso().getNombre(),
                item.getRecurso().getUnidadMedida(),
                item.getCantidadRequerida()
        );
    }
}
