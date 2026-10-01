package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.AporteOngResponse;
import com.proyecto.backend.dto.AporteRecursoResponse;
import com.proyecto.backend.dto.OfertaResponse;
import com.proyecto.backend.dto.OfertaVersionResponse;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.Oferta;
import com.proyecto.backend.model.OfertaVersion;
import com.proyecto.backend.model.Ong;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        List<AporteRecursoResponse> aportes = agruparPorItem(oferta.getDetalles().stream()
                .distinct()
                .map(d -> new Celda(d.getItemLote(), d.getOng(), d.getCantidadOfrecida()))
                .toList());
        List<com.proyecto.backend.dto.OngResponse> ongs = oferta.getOngs().stream()
                .sorted(Comparator.comparing(o -> o.getRazonSocial().toLowerCase()))
                .map(ongMapper::toResponse)
                .toList();

        return new OfertaResponse(
                oferta.getId(),
                oferta.getEstado().name(),
                oferta.getEstado().getEtiqueta(),
                oferta.getFechaOferta(),
                oferta.getFechaModificacion(),
                oferta.getNumeroVersion(),
                oferta.getLote().getId(),
                oferta.getLote().getTitulo(),
                oferta.getLote().getEmergencia().getZonaAfectada(),
                ongs,
                aportes
        );
    }

    public OfertaVersionResponse toVersionResponse(OfertaVersion version) {
        List<Celda> celdas = version.getDetalles().stream()
                .map(d -> new Celda(d.getItemLote(), d.getOng(), d.getCantidadOfrecida()))
                .toList();
        return new OfertaVersionResponse(
                version.getNumero(),
                version.getTipoCambio().name(),
                version.getTipoCambio().getEtiqueta(),
                version.getEstado().name(),
                version.getEstado().getEtiqueta(),
                version.getFecha(),
                version.getUsuario(),
                version.getOng().getId(),
                version.getOng().getRazonSocial(),
                agruparPorItem(celdas)
        );
    }

    private List<AporteRecursoResponse> agruparPorItem(List<Celda> celdas) {
        Map<Long, List<Celda>> porItem = new LinkedHashMap<>();
        for (Celda celda : celdas) {
            porItem.computeIfAbsent(celda.itemLote().getId(), id -> new ArrayList<>()).add(celda);
        }
        return porItem.values().stream().map(this::toAporteRecursoResponse).toList();
    }

    private AporteRecursoResponse toAporteRecursoResponse(List<Celda> celdas) {
        ItemLote item = celdas.get(0).itemLote();
        List<AporteOngResponse> porOng = celdas.stream()
                .map(celda -> new AporteOngResponse(
                        celda.ong().getId(),
                        celda.ong().getRazonSocial(),
                        celda.cantidad()
                ))
                .toList();
        int total = celdas.stream().mapToInt(Celda::cantidad).sum();

        return new AporteRecursoResponse(
                item.getId(),
                item.getRecurso().getNombre(),
                item.getRecurso().getUnidadMedida(),
                item.getCantidadRequerida(),
                total,
                porOng
        );
    }

    /** Celda item x ONG, común a la oferta vigente y a sus versiones. */
    private record Celda(ItemLote itemLote, Ong ong, Integer cantidad) {
    }
}
