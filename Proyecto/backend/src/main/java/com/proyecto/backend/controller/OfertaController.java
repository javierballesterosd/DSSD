package com.proyecto.backend.controller;

import com.proyecto.backend.dto.OfertaRequest;
import com.proyecto.backend.dto.OfertaResponse;
import com.proyecto.backend.service.AuthService;
import com.proyecto.backend.service.OfertaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ofertas")
public class OfertaController {

    private final OfertaService ofertaService;
    private final AuthService authService;

    public OfertaController(OfertaService ofertaService, AuthService authService) {
        this.ofertaService = ofertaService;
        this.authService = authService;
    }

    /** Ofertas de un lote en las que participa la ONG del usuario logueado. */
    @GetMapping("/mias")
    public List<OfertaResponse> misOfertas(@RequestParam Long loteId, HttpSession session) {
        Long ongIdUsuario = authService.ongDelUsuario(session);
        return ofertaService.listarDeOng(loteId, ongIdUsuario);
    }

    @PostMapping
    public ResponseEntity<OfertaResponse> registrar(@Valid @RequestBody OfertaRequest request,
                                                    HttpSession session) {
        Long ongIdUsuario = authService.ongDelUsuario(session);
        OfertaResponse creada = ofertaService.registrar(request, ongIdUsuario);
        return ResponseEntity.created(java.net.URI.create("/api/ofertas/" + creada.id())).body(creada);
    }
}
