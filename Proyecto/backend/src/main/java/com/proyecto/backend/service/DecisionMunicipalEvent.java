package com.proyecto.backend.service;

import com.proyecto.backend.dto.auth.LoginResponse;
import java.util.Set;

public record DecisionMunicipalEvent(Long emergenciaId, boolean aceptada, String municipioGroupPath,
        Set<String> ongGroupPaths, LoginResponse remitente) {}
