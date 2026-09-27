package com.proyecto.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OfertaResponse(
        Long id,
        String estado,
        LocalDateTime fechaOferta,
        Long loteId,
        String loteTitulo,
        String emergenciaZona,
        List<OngResponse> ongs,
        List<AporteRecursoResponse> aportes
) {
}
