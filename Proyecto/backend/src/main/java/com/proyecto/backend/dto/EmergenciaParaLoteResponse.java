package com.proyecto.backend.dto;

import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.NivelGravedad;

import java.time.LocalDateTime;

/** Emergencia en la lista del coordinador, con su último lote (null si todavía no tiene). */
public record EmergenciaParaLoteResponse(
        Long id,
        String descripcion,
        NivelGravedad nivelGravedad,
        String zonaAfectada,
        Long municipioId,
        LocalDateTime fechaRegistro,
        Long loteId,
        EstadoLote estadoLote
) {
}
