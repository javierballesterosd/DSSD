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
import com.proyecto.backend.dto.lote.EmergenciaLoteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

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
                remitente,
                authService.bonitaSession(session)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/para-lotes")
    public ResponseEntity<Page<EmergenciaLoteResponseDTO>> obtenerEmergenciasParaLotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                emergenciaService.obtenerEmergenciasParaLotes(page, size)
        );
    }
}