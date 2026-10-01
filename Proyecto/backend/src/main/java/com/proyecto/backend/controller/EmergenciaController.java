package com.proyecto.backend.controller;

import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.EmergenciaParaLoteResponse;
import com.proyecto.backend.dto.EmergenciaRequest;
import com.proyecto.backend.dto.EmergenciaResponse;
import com.proyecto.backend.service.AuthService;
import com.proyecto.backend.service.EmergenciaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/emergencias")
@RequiredArgsConstructor
public class EmergenciaController {

    private final EmergenciaService emergenciaService;
    private final AuthService authService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmergenciaResponse registrarEmergencia(
            @Valid @RequestBody EmergenciaRequest request, HttpSession session) {
        // El municipio sale del usuario logueado, no del request
        Long municipioId = authService.municipioDelUsuario(session);
        LoginResponse remitente = authService.currentUser(session);
        return emergenciaService.registrarEmergencia(
                request,
                municipioId,
                remitente,
                authService.bonitaSession(session)
        );
    }

    @GetMapping("/para-lotes")
    public Page<EmergenciaParaLoteResponse> obtenerEmergenciasParaLotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session
    ) {
        // El coordinador solo trabaja con las emergencias de los municipios de su región
        return emergenciaService.obtenerEmergenciasParaLotes(
                authService.regionDelUsuario(session), page, size);
    }

    /** Emergencias del municipio del operador logueado. */
    @GetMapping("/mias")
    public List<EmergenciaParaLoteResponse> misEmergencias(HttpSession session) {
        return emergenciaService.listarDeMunicipio(authService.municipioDelUsuario(session));
    }

    @GetMapping("/{id}")
    public EmergenciaParaLoteResponse obtenerDetalle(@PathVariable Long id, HttpSession session) {
        LoginResponse usuario = authService.requerirRol(session, "MUNICIPAL", "COORDINADOR", "AUDITOR");
        return emergenciaService.obtenerDetalle(id, usuario);
    }
}
