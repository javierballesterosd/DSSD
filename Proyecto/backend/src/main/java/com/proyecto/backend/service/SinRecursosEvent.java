package com.proyecto.backend.service;

import java.util.Set;

/** El SN no habilitó recursos en ninguna oferta del lote: el municipio debe volver a registrar la emergencia. */
public record SinRecursosEvent(Long emergenciaId, String emergenciaTitulo, String municipioGroupPath,
        Set<String> ongGroupPaths) {}
