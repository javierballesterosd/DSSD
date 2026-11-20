package com.proyecto.backend.dto;

import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.NivelGravedad;

import java.time.LocalDateTime;
import java.util.List;

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
        EstadoLote estadoLote,
        boolean requiereActualizacion,
        // true si hay que volver a registrar porque el SN no habilitó recursos (y no porque el municipio rechazó)
        boolean sinRecursos,
        // Solo si el último lote está cancelado: SIN_RECURSOS (el SN no habilitó nada) o RECHAZO_MUNICIPAL
        String motivoLoteCancelado,
        List<ActividadResponse> actividades
) {
}
