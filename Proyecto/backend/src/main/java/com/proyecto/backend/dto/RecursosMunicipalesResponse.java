package com.proyecto.backend.dto;

import java.util.List;

/** Solo cantidades habilitadas por SN, sin datos de las ONGs. */
public record RecursosMunicipalesResponse(Long loteId, Boolean decision, boolean confirmada,
        boolean puedeDecidir, String mensaje, List<RecursoAceptado> recursos) {
    public record RecursoAceptado(Long recursoId, String nombre, String unidadMedida, long cantidad) {}
}
