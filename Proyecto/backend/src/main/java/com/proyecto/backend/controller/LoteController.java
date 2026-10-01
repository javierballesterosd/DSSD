package com.proyecto.backend.controller;

import com.proyecto.backend.dto.LoteDetalleResponse;
import com.proyecto.backend.dto.LoteResumenResponse;
import com.proyecto.backend.dto.lote.LoteRequestDTO;
import com.proyecto.backend.dto.lote.LoteResponseDTO;
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
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;
    private final AuthService authService;

    @PostMapping("/api/emergencias/{emergenciaId}/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    public LoteResponseDTO publicarLote(
            @PathVariable Long emergenciaId,
            @Valid @RequestBody LoteRequestDTO requestDTO,
            HttpSession session
    ) {
        return loteService.publicarLote(
                emergenciaId, requestDTO, authService.bonitaSession(session));
    }

    @GetMapping("/api/lotes")
    public List<LoteResumenResponse> listar(
            @RequestParam(
                    name = "estado",
                    required = false,
                    defaultValue = "ACTIVO"
            ) EstadoLote estado
    ) {
        return loteService.listarPublicados(estado);
    }

    @GetMapping("/api/lotes/{id}")
    public LoteDetalleResponse obtenerDetalle(
            @PathVariable Long id
    ) {
        return loteService.obtenerDetalle(id);
    }
}