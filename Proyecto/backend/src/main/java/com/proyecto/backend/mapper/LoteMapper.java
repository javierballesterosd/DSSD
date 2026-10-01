package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.LoteRequest;
import com.proyecto.backend.dto.EmergenciaResumenResponse;
import com.proyecto.backend.dto.ItemLoteResponse;
import com.proyecto.backend.dto.LoteDetalleResponse;
import com.proyecto.backend.dto.LoteResumenResponse;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.Lote;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class LoteMapper {
    private final ItemLoteMapper itemLoteMapper;

    public LoteMapper(ItemLoteMapper itemLoteMapper) {
        this.itemLoteMapper = itemLoteMapper;
    }

    public LoteResumenResponse toResumenResponse(Lote lote) {
        return new LoteResumenResponse(
                lote.getId(),
                lote.getTitulo(),
                lote.getEstado().name(),
                lote.getFechaCreacion(),
                lote.getFechaAperturaOfertas(),
                lote.getFechaCierreOfertas(),
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
                lote.getFechaAperturaOfertas(),
                lote.getFechaCierreOfertas(),
                toEmergenciaResumen(lote.getEmergencia()),
                toItemResponses(lote),
                esConvocatoriaAbierta(lote)
        );
    }

    private boolean esConvocatoriaAbierta(Lote lote) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime apertura = lote.getFechaAperturaOfertas();
        LocalDateTime cierre = lote.getFechaCierreOfertas();
        return (apertura == null || !apertura.isAfter(ahora)) && (cierre == null || cierre.isAfter(ahora));
    }

    private EmergenciaResumenResponse toEmergenciaResumen(Emergencia emergencia) {
        return new EmergenciaResumenResponse(
                emergencia.getId(),
                emergencia.getZonaAfectada(),
                emergencia.getNivelGravedad().name(),
                emergencia.getNivelGravedad().getEtiqueta(),
                emergencia.getDescripcion(),
                emergencia.getFechaRegistro(),
                emergencia.getMunicipio().getNombre()
        );
    }

    private List<ItemLoteResponse> toItemResponses(Lote lote) {
        return lote.getItems().stream().map(itemLoteMapper::toResponse).toList();
    }

    public Lote toEntity(LoteRequest request, Emergencia emergencia) {
        return Lote.builder()
                .titulo(request.titulo())
                .estado(EstadoLote.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .fechaAperturaOfertas(request.fechaAperturaOfertas())
                .fechaCierreOfertas(request.fechaCierreOfertas())
                .emergencia(emergencia)
                .build();
    }
}
