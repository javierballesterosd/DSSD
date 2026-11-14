package com.proyecto.backend.dto;

public record AporteOngResponse(
        Long ongId,
        String ongCodigo,
        String razonSocial,
        Integer cantidadOfrecida
) {
}
