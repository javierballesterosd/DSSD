package com.proyecto.backend.controller;

import com.proyecto.backend.dto.IntegracionNacional;
import com.proyecto.backend.exception.UnauthenticatedException;
import com.proyecto.backend.security.BonitaTokenValidator;
import com.proyecto.backend.service.AuthService;
import com.proyecto.backend.service.DespliegueService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergencias/{id}")
@RequiredArgsConstructor
public class DespliegueController {
    private final DespliegueService despliegue;
    private final AuthService auth;
    private final BonitaTokenValidator tokens;

    @PostMapping("/monitoreo/completar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void completar(@PathVariable Long id, HttpSession sesion) {
        despliegue.completarMonitoreo(id, auth.regionDelUsuario(sesion), auth.bonitaSession(sesion));
    }

    @PostMapping("/despliegue/validacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validar(@PathVariable Long id, HttpSession sesion) {
        despliegue.validarDespliegue(id, auth.regionDelUsuario(sesion), auth.bonitaSession(sesion));
    }

    @GetMapping("/despliegue/integracion-nacional")
    public IntegracionNacional.Plan plan(@PathVariable Long id, @RequestParam Long loteId, @RequestParam String caseId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        autorizar(authorization);
        return despliegue.plan(id, loteId, caseId);
    }

    @PostMapping("/despliegue/confirmacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmar(@PathVariable Long id, @Valid @RequestBody IntegracionNacional.Respuesta respuesta,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        autorizar(authorization);
        despliegue.confirmar(id, respuesta);
    }

    private void autorizar(String authorization) {
        if (!tokens.isValid(authorization)) throw new UnauthenticatedException("Token de Bonita inválido o ausente");
    }
}
