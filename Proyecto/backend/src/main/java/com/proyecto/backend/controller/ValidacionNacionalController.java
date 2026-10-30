package com.proyecto.backend.controller;

import com.proyecto.backend.dto.IntegracionNacional;
import com.proyecto.backend.exception.UnauthenticatedException;
import com.proyecto.backend.security.BonitaTokenValidator;
import com.proyecto.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergencias/{id}")
@RequiredArgsConstructor
public class ValidacionNacionalController {
    private final ValidacionNacionalService validaciones;
    private final DecisionMunicipalService decisiones;
    private final BonitaTokenValidator tokens;

    @GetMapping("/integracion-nacional")
    public IntegracionNacional.Plan plan(@PathVariable Long id, @RequestParam Long loteId, @RequestParam String caseId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        autorizar(authorization);
        return validaciones.plan(id, loteId, caseId);
    }

    @PostMapping("/validacion-sn")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validar(@PathVariable Long id, @Valid @RequestBody IntegracionNacional.Respuesta respuesta,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        autorizar(authorization);
        validaciones.registrar(id, respuesta);
    }

    @PostMapping("/decision-municipal/confirmacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmar(@PathVariable Long id, @Valid @RequestBody IntegracionNacional.Respuesta respuesta,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        autorizar(authorization);
        decisiones.confirmar(id, respuesta);
    }

    private void autorizar(String authorization) {
        if (!tokens.isValid(authorization)) throw new UnauthenticatedException("Token de Bonita inválido o ausente");
    }
}
