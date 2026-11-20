package com.proyecto.backend.service;

import com.proyecto.backend.dto.auth.LoginResponse; 
import java.util.List;

public record OfertaAceptadaEvent(
        Long ofertaId,
        Long emergenciaId,
        String zonaAfectada,
        String municipioNombre,
        List<String> ongsGroupPaths,
        LoginResponse remitente
) {}