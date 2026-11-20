package com.proyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record AporteOngResponse(
        Long ongId,
        String ongCodigo,
        String razonSocial,
        Integer cantidadOfrecida,
        // Lo que habilitó el SN; null mientras la oferta no fue validada (y en las versiones del historial)
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer cantidadAceptada
) {
    public AporteOngResponse(Long ongId, String ongCodigo, String razonSocial, Integer cantidadOfrecida) {
        this(ongId, ongCodigo, razonSocial, cantidadOfrecida, null);
    }
}
