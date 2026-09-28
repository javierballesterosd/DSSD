package com.proyecto.backend.controller;

import com.proyecto.backend.dto.emergencia.EmergenciaRequestDTO;
import com.proyecto.backend.dto.emergencia.EmergenciaResponseDTO;
import com.proyecto.backend.service.EmergenciaService;
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

    @PostMapping
    public ResponseEntity<EmergenciaResponseDTO> registrarEmergencia(
            @Valid @RequestBody EmergenciaRequestDTO requestDTO) {
        EmergenciaResponseDTO respuesta = emergenciaService.registrarEmergencia(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}