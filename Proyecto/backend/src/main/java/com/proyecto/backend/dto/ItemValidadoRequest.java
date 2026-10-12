package com.proyecto.backend.dto;

public record ItemValidadoRequest(
    Long detalleOfertaId,
    Integer cantidadAceptada
) {}