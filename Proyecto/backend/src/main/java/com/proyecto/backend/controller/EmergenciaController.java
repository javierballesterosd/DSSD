package com.proyecto.backend.controller;

import com.proyecto.backend.dto.emergencia.EmergenciaRequestDTO;
import com.proyecto.backend.dto.emergencia.EmergenciaResponseDTO;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.service.AuthService;
import com.proyecto.backend.service.EmergenciaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/emergencias")
@RequiredArgsConstructor
public class EmergenciaController {

    private final EmergenciaService emergenciaService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<EmergenciaResponseDTO> registrarEmergencia(
            @Valid @RequestBody EmergenciaRequestDTO requestDTO, HttpSession session) {
        // El municipio sale del usuario logueado, no del request
        Long municipioId = authService.municipioDelUsuario(session);
        LoginResponse remitente = authService.currentUser(session);
        EmergenciaResponseDTO respuesta = emergenciaService.registrarEmergencia(
                requestDTO,
                municipioId,
                authService.bonitaSession(session),
                remitente
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}