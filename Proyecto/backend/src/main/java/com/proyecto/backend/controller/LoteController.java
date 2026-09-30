package com.proyecto.backend.controller;

import com.proyecto.backend.dto.LoteDetalleResponse;
import com.proyecto.backend.dto.LoteResumenResponse;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.dto.lote.LoteRequestDTO;
import com.proyecto.backend.dto.lote.LoteResponseDTO;
import com.proyecto.backend.service.LoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoteResponseDTO publicarLote(
            @PathVariable Long emergenciaId,
            @Valid @RequestBody LoteRequestDTO requestDTO
    ) {
        return loteService.publicarLote(emergenciaId, requestDTO);
    }

    @GetMapping
    public List<LoteResumenResponse> listar(
            @RequestParam(name = "estado", required = false, defaultValue = "ACTIVO") EstadoLote estado) {
        return loteService.listarPublicados(estado);
    }

    @GetMapping("/{id}")
    public LoteDetalleResponse obtenerDetalle(@PathVariable Long id) {
        return loteService.obtenerDetalle(id);
    }
}