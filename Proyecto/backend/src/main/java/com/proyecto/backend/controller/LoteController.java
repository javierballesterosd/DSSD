package com.proyecto.backend.controller;

import com.proyecto.backend.dto.LoteDetalleResponse;
import com.proyecto.backend.dto.LoteResumenResponse;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.service.LoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
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
