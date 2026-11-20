
package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.ActividadResponse;
import com.proyecto.backend.dto.EmergenciaParaLoteResponse;
import com.proyecto.backend.dto.EmergenciaRequest;
import com.proyecto.backend.dto.EmergenciaResponse;
import com.proyecto.backend.model.Actividad;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.model.Ong;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public EmergenciaParaLoteResponse toParaLoteResponse(
            Emergencia emergencia,
            Optional<Lote> ultimoLote) {

        List<ActividadResponse> actividades = emergencia.getActividades().stream()
                .map(this::toActividadResponse)
                .toList();

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
                ultimoLote.map(Lote::getEstado).orElse(null),
                emergencia.isRequiereActualizacion(),
                emergencia.isRequiereActualizacion() && ultimoLote.map(l -> l.getEstado() == EstadoLote.CANCELADO
                        && l.getDecisionMunicipal() == null).orElse(false),
                motivoLoteCancelado(ultimoLote),
                actividades
        );
    }

    // Un lote se cancela porque el SN no habilitó recursos (no hay decisión municipal) o porque el municipio rechazó
    private String motivoLoteCancelado(Optional<Lote> ultimoLote) {
        return ultimoLote.filter(l -> l.getEstado() == EstadoLote.CANCELADO)
                .map(l -> l.getDecisionMunicipal() == null ? "SIN_RECURSOS" : "RECHAZO_MUNICIPAL")
                .orElse(null);
    }

    private ActividadResponse toActividadResponse(Actividad actividad) {
        List<ActividadResponse.AvanceOferta> avance = actividad.getAsignaciones().stream()
                .map(a -> new ActividadResponse.AvanceOferta(
                        a.getOferta().getId(),
                        a.getOferta().getOngs().stream().map(Ong::getRazonSocial).sorted().toList(),
                        a.isFinalizada(),
                        a.getFechaFinalizacion(),
                        a.getOngFinalizacion() != null ? a.getOngFinalizacion().getRazonSocial() : null))
                .toList();
        return new ActividadResponse(
                actividad.getId(),
                actividad.getNombre(),
                actividad.getDescripcion(),
                actividad.isFinalizada(),
                avance.size(),
                (int) avance.stream().filter(ActividadResponse.AvanceOferta::finalizada).count(),
                avance
        );
    }
}
