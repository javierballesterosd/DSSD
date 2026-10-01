package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.EmergenciaParaLoteResponse;
import com.proyecto.backend.dto.EmergenciaRequest;
import com.proyecto.backend.dto.EmergenciaResponse;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.model.Municipio;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmergenciaMapper {

    public Emergencia toEntity(EmergenciaRequest request, Municipio municipio) {
        return Emergencia.builder()
                .nivelGravedad(request.nivelGravedad())
                .zonaAfectada(request.zonaAfectada())
                .descripcion(request.descripcion())
                .municipio(municipio)
                .build();
    }

    public EmergenciaResponse toResponse(Emergencia entity) {
        return new EmergenciaResponse(
                entity.getId(),
                entity.getNivelGravedad(),
                entity.getZonaAfectada(),
                entity.getDescripcion(),
                entity.getFechaRegistro(),
                entity.getBonitaCaseId(),
                entity.getMunicipio() != null ? entity.getMunicipio().getId() : null
        );
    }

    public EmergenciaParaLoteResponse toParaLoteResponse(Emergencia emergencia, Optional<Lote> ultimoLote) {
        return new EmergenciaParaLoteResponse(
                emergencia.getId(),
                emergencia.getDescripcion(),
                emergencia.getNivelGravedad(),
                emergencia.getNivelGravedad().getEtiqueta(),
                emergencia.getZonaAfectada(),
                emergencia.getMunicipio().getId(),
                emergencia.getMunicipio().getNombre(),
                emergencia.getFechaRegistro(),
                ultimoLote.map(Lote::getId).orElse(null),
                ultimoLote.map(Lote::getEstado).orElse(null)
        );
    }
}
