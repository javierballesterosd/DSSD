package com.proyecto.backend.controller;

import com.proyecto.backend.dto.InventarioOngResponse;
import com.proyecto.backend.dto.OngResponse;
import com.proyecto.backend.service.OngService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/ongs")
@RequiredArgsConstructor
public class OngController {

    private final OngService ongService;

    @GetMapping
    public List<OngResponse> listar() {
        return ongService.listar();
    }

    @GetMapping("/inventario")
    public List<InventarioOngResponse> inventario(@RequestParam("ongIds") Set<Long> ongIds) {
        return ongService.inventarioPorOng(ongIds);
    }
}
