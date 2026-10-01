package com.proyecto.backend.dto;

import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.NivelGravedad;

import java.time.LocalDateTime;

/** Emergencia en los listados y el detalle de municipal y coordinador, con su último lote (null si todavía no tiene). */
public record EmergenciaParaLoteResponse(
        Long id,
        String descripcion,
        NivelGravedad nivelGravedad,
        String nivelGravedadEtiqueta,
        String zonaAfectada,
        Long municipioId,
        String municipio,
        LocalDateTime fechaRegistro,
        Long loteId,
        EstadoLote estadoLote
) {
}
