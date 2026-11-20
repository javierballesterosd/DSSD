package com.proyecto.backend.service;

import com.proyecto.backend.dto.auth.LoginResponse;
import java.util.Set;

public record DespliegueFinalizadoEvent(Long emergenciaId, String municipioGroupPath,
        Set<String> ongGroupPaths, String descripcionCierre, LoginResponse remitente) {}
