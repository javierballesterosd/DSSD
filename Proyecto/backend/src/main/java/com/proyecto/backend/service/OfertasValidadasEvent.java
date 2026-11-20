package com.proyecto.backend.service;

/** El SN terminó de validar las ofertas del lote y quedaron recursos habilitados: el municipio debe decidir. */
public record OfertasValidadasEvent(Long emergenciaId, String emergenciaTitulo, String municipioGroupPath) {}
