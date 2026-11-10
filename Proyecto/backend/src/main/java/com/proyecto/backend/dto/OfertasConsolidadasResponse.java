package com.proyecto.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OfertasConsolidadasResponse(
        Long emergenciaId,
        Long loteId,
        List<OfertaConsolidadaResponse> ofertas
) {

    public record OfertaConsolidadaResponse(
            Long id,
            String estado,
            LocalDateTime fechaOferta,
            LocalDateTime fechaModificacion,
            Integer numeroVersion,
            List<OngResponse> ongs,
            List<AporteConsolidadoResponse> aportes
    ) {
    }

    public record AporteConsolidadoResponse(
            Long itemLoteId,
            String recursoNombre,
            Integer cantidadRequerida,
            Integer totalOfrecido,
            List<AporteOngResponse> porOng
    ) {
    }
}
