package com.proyecto.backend.controller;

import com.proyecto.backend.model.Recurso;
import com.proyecto.backend.service.RecursoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recursos")
@RequiredArgsConstructor
public class RecursoController {

    private final RecursoService recursoService;

    @GetMapping
    public List<Recurso> obtenerRecursos() {
        return recursoService.obtenerRecursos();
    }
}