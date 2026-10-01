package com.proyecto.backend.controller;

import com.proyecto.backend.dto.LoteDetalleResponse;
import com.proyecto.backend.dto.LoteResumenResponse;
import com.proyecto.backend.dto.LoteRequest;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.service.AuthService;
import com.proyecto.backend.service.LoteService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;
    private final AuthService authService;

    @PostMapping("/emergencias/{emergenciaId}/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    public LoteDetalleResponse publicarLote(
            @PathVariable Long emergenciaId,
            @Valid @RequestBody LoteRequest request,
            HttpSession session
    ) {
        return loteService.publicarLote(
                emergenciaId, request, authService.bonitaSession(session));
    }

    @GetMapping("/lotes")
    public List<LoteResumenResponse> listar(
            @RequestParam(
                    name = "estado",
                    required = false,
                    defaultValue = "ACTIVO"
            ) EstadoLote estado
    ) {
        return loteService.listarPublicados(estado);
    }

    @GetMapping("/lotes/{id}")
    public LoteDetalleResponse obtenerDetalle(
            @PathVariable Long id
    ) {
        return loteService.obtenerDetalle(id);
    }
}