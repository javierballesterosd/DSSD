package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.EmergenciaResumenResponse;
import com.proyecto.backend.dto.ItemLoteResponse;
import com.proyecto.backend.dto.LoteDetalleResponse;
import com.proyecto.backend.dto.LoteResumenResponse;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.Lote;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class LoteMapper {

    public LoteResumenResponse toResumenResponse(Lote lote) {
        return new LoteResumenResponse(
                lote.getId(),
                lote.getTitulo(),
                lote.getEstado().name(),
                lote.getFechaCreacion(),
                lote.getFechaInicio(),
                toEmergenciaResumen(lote.getEmergencia()),
                toItemResponses(lote)
        );
    }

    public LoteDetalleResponse toDetalleResponse(Lote lote) {
        return new LoteDetalleResponse(
                lote.getId(),
                lote.getTitulo(),
                lote.getEstado().name(),
                lote.getFechaCreacion(),
                lote.getFechaInicio(),
                toEmergenciaResumen(lote.getEmergencia()),
                toItemResponses(lote),
                esConvocatoriaAbierta(lote.getEmergencia())
        );
    }

    private boolean esConvocatoriaAbierta(Emergencia emergencia) {
        LocalDateTime cierre = emergencia.getFechaCierreOfertas();
        return cierre == null || cierre.isAfter(LocalDateTime.now());
    }

    private EmergenciaResumenResponse toEmergenciaResumen(Emergencia emergencia) {
        return new EmergenciaResumenResponse(
                emergencia.getId(),
                emergencia.getZonaAfectada(),
                emergencia.getNivelGravedad().name(),
                emergencia.getNivelGravedad().getEtiqueta(),
                emergencia.getDescripcion(),
                emergencia.getFechaRegistro(),
                emergencia.getMunicipio().getNombre(),
                emergencia.getFechaCierreOfertas()
        );
    }

    private List<ItemLoteResponse> toItemResponses(Lote lote) {
        return lote.getItems().stream().map(this::toItemResponse).toList();
    }

    private ItemLoteResponse toItemResponse(ItemLote item) {
        return new ItemLoteResponse(
                item.getId(),
                item.getRecurso().getId(),
                item.getRecurso().getNombre(),
                item.getRecurso().getUnidadMedida(),
                item.getCantidadRequerida()
        );
    }
}
