package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.AporteOngResponse;
import com.proyecto.backend.dto.AporteRecursoResponse;
import com.proyecto.backend.dto.OfertaResponse;
import com.proyecto.backend.model.DetalleOferta;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.Oferta;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OfertaMapper {

    private final OngMapper ongMapper;

    public OfertaMapper(OngMapper ongMapper) {
        this.ongMapper = ongMapper;
    }

    public OfertaResponse toResponse(Oferta oferta) {
        List<AporteRecursoResponse> aportes = agruparPorItem(oferta.getDetalles());
        List<com.proyecto.backend.dto.OngResponse> ongs = oferta.getOngs().stream()
                .sorted(Comparator.comparing(o -> o.getRazonSocial().toLowerCase()))
                .map(ongMapper::toResponse)
                .toList();

        return new OfertaResponse(
                oferta.getId(),
                oferta.getEstado().name(),
                oferta.getEstado().getEtiqueta(),
                oferta.getFechaOferta(),
                oferta.getLote().getId(),
                oferta.getLote().getTitulo(),
                oferta.getLote().getEmergencia().getZonaAfectada(),
                ongs,
                aportes
        );
    }

    private List<AporteRecursoResponse> agruparPorItem(List<DetalleOferta> detalles) {
        Map<Long, List<DetalleOferta>> porItem = new LinkedHashMap<>();
        for (DetalleOferta detalle : detalles) {
            porItem.computeIfAbsent(detalle.getItemLote().getId(), id -> new java.util.ArrayList<>()).add(detalle);
        }

        return porItem.values().stream().map(this::toAporteRecursoResponse).toList();
    }

    private AporteRecursoResponse toAporteRecursoResponse(List<DetalleOferta> detalles) {
        ItemLote item = detalles.get(0).getItemLote();
        List<AporteOngResponse> porOng = detalles.stream()
                .map(detalle -> new AporteOngResponse(
                        detalle.getOng().getId(),
                        detalle.getOng().getRazonSocial(),
                        detalle.getCantidadOfrecida()
                ))
                .toList();
        int total = detalles.stream().mapToInt(DetalleOferta::getCantidadOfrecida).sum();

        return new AporteRecursoResponse(
                item.getId(),
                item.getRecurso().getNombre(),
                item.getRecurso().getUnidadMedida(),
                item.getCantidadRequerida(),
                total,
                porOng
        );
    }
}
