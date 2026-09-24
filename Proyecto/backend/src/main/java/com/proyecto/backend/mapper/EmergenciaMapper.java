package com.proyecto.backend.mapper;

import com.proyecto.backend.model.dto.request.EmergenciaRequestDTO;
import com.proyecto.backend.model.dto.response.EmergenciaResponseDTO;
import com.proyecto.backend.model.entity.Emergencia;
import com.proyecto.backend.model.entity.Municipio;
import org.springframework.stereotype.Component;

@Component
public class EmergenciaMapper {

    public Emergencia toEntity(EmergenciaRequestDTO dto, Municipio municipio) {
        return Emergencia.builder()
                .nivelGravedad(dto.getNivelGravedad())
                .zonaAfectada(dto.getZonaAfectada())
                .descripcion(dto.getDescripcion())
                .municipio(municipio)
                .build();
    }

    public EmergenciaResponseDTO toDto(Emergencia entity) {
        return EmergenciaResponseDTO.builder()
                .id(entity.getId())
                .nivelGravedad(entity.getNivelGravedad())
                .zonaAfectada(entity.getZonaAfectada())
                .descripcion(entity.getDescripcion())
                .fechaRegistro(entity.getFechaRegistro())
                .bonitaCaseId(entity.getBonitaCaseId())
                .municipioId(entity.getMunicipio() != null ? entity.getMunicipio().getId() : null)
                .build();
    }
}