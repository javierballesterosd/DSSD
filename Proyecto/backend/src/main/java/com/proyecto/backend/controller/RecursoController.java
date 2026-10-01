package com.proyecto.backend.controller;

import com.proyecto.backend.dto.RecursoResponse;
import com.proyecto.backend.service.AuthService;
import com.proyecto.backend.service.RecursoService;
import jakarta.servlet.http.HttpSession;
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
    private final AuthService authService;

    @GetMapping
    public List<RecursoResponse> obtenerRecursos(HttpSession session) {
        // Catálogo común: alcanza con estar logueado
        authService.currentUser(session);
        return recursoService.obtenerRecursos();
    }
}