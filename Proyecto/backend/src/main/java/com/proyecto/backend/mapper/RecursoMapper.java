package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.RecursoResponse;
import com.proyecto.backend.model.Recurso;
import org.springframework.stereotype.Component;

@Component
public class RecursoMapper {

    public RecursoResponse toResponse(Recurso recurso) {
        return new RecursoResponse(recurso.getId(), recurso.getNombre(), recurso.getUnidadMedida());
    }
}
