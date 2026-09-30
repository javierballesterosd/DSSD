package com.proyecto.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OfertaVersionResponse(
        Integer numero,
        String tipoCambio,
        String tipoCambioEtiqueta,
        String estado,
        String estadoEtiqueta,
        LocalDateTime fecha,
        String usuario,
        Long ongId,
        String ongNombre,
        List<AporteRecursoResponse> aportes
) {
}
