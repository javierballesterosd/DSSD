package com.proyecto.backend.dto;

public record ItemValidadoRequest(
    Long detalleOfertaId,
    Integer cantidadAceptada,
    Long itemLoteId,
    Long ongId
) {
    public ItemValidadoRequest(Long detalleOfertaId, Integer cantidadAceptada) {
        this(detalleOfertaId, cantidadAceptada, null, null);
    }
}
