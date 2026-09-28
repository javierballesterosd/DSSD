package com.proyecto.backend.controller;

import com.proyecto.backend.dto.OfertaRequest;
import com.proyecto.backend.dto.OfertaResponse;
import com.proyecto.backend.service.OfertaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ofertas")
public class OfertaController {

    private final OfertaService ofertaService;

    public OfertaController(OfertaService ofertaService) {
        this.ofertaService = ofertaService;
    }

    @PostMapping
    public ResponseEntity<OfertaResponse> registrar(@Valid @RequestBody OfertaRequest request) {
        OfertaResponse creada = ofertaService.registrar(request);
        return ResponseEntity.created(java.net.URI.create("/api/ofertas/" + creada.id())).body(creada);
    }
}
