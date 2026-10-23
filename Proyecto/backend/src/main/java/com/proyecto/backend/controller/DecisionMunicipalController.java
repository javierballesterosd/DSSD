package com.proyecto.backend.controller;

import com.proyecto.backend.dto.*;
import com.proyecto.backend.service.AuthService;
import com.proyecto.backend.service.DecisionMunicipalService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergencias/{id}")
@RequiredArgsConstructor
public class DecisionMunicipalController {
    private final DecisionMunicipalService decisiones;
    private final AuthService auth;

    @GetMapping("/recursos-aceptados")
    public RecursosMunicipalesResponse recursos(@PathVariable Long id, HttpSession session) {
        return decisiones.consultar(id, auth.municipioDelUsuario(session));
    }

    @PostMapping("/decision-municipal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decidir(@PathVariable Long id, @Valid @RequestBody DecisionMunicipalRequest request, HttpSession session) {
        auth.municipioDelUsuario(session);
        decisiones.decidir(id, request, auth.currentUser(session), auth.bonitaSession(session));
    }

    @PutMapping
    public EmergenciaResponse actualizar(@PathVariable Long id, @Valid @RequestBody EmergenciaRequest request, HttpSession session) {
        auth.municipioDelUsuario(session);
        return decisiones.actualizar(id, request, auth.currentUser(session), auth.bonitaSession(session));
    }
}
