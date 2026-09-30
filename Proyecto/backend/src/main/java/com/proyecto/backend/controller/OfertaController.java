package com.proyecto.backend.controller;

import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.OfertaEdicionRequest;
import com.proyecto.backend.dto.OfertaRequest;
import com.proyecto.backend.dto.OfertaResponse;
import com.proyecto.backend.dto.OfertaVersionResponse;
import com.proyecto.backend.service.AuthService;
import com.proyecto.backend.service.OfertaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    /** Ids de los lotes donde la ONG del usuario logueado ya ofertó. */
    @GetMapping("/mias/lotes")
    public List<Long> lotesConMisOfertas(HttpSession session) {
        return ofertaService.lotesConOfertaDeOng(authService.ongDelUsuario(session));
    }

    /** Todas las ofertas, incluidas las eliminadas (solo auditor). */
    @GetMapping
    public List<OfertaResponse> todas(@RequestParam(required = false) Long loteId, HttpSession session) {
        authService.auditorDelUsuario(session);
        return ofertaService.listarTodas(loteId);
    }

    /** Historial de versiones de una oferta (ONGs participantes y auditor). */
    @GetMapping("/{id}/versiones")
    public List<OfertaVersionResponse> versiones(@PathVariable Long id, HttpSession session) {
        LoginResponse usuario = authService.currentUser(session);
        return ofertaService.listarVersiones(id, usuario.getRole(), usuario.getOngId());
    }

    @PostMapping
    public ResponseEntity<OfertaResponse> registrar(@Valid @RequestBody OfertaRequest request,
                                                    HttpSession session) {
        Long ongIdUsuario = authService.ongDelUsuario(session);
        OfertaResponse creada = ofertaService.registrar(request, ongIdUsuario, username(session));
        return ResponseEntity.created(java.net.URI.create("/api/ofertas/" + creada.id())).body(creada);
    }

    /** Edita las cantidades de una oferta pendiente, dentro de la ventana del lote. */
    @PutMapping("/{id}")
    public OfertaResponse actualizar(@PathVariable Long id, @Valid @RequestBody OfertaEdicionRequest request,
                                     HttpSession session) {
        Long ongIdUsuario = authService.ongDelUsuario(session);
        return ofertaService.actualizar(id, request, ongIdUsuario, username(session));
    }

    /** Baja lógica de una oferta pendiente, dentro de la ventana del lote. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpSession session) {
        Long ongIdUsuario = authService.ongDelUsuario(session);
        ofertaService.eliminar(id, ongIdUsuario, username(session));
        return ResponseEntity.noContent().build();
    }

    private String username(HttpSession session) {
        return authService.currentUser(session).getUsername();
    }
}
