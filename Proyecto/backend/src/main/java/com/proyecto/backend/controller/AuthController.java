package com.proyecto.backend.controller;

import com.proyecto.backend.dto.auth.LoginRequest;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController //indica que la clase maneja HTTP requests y que devuelve respuestas en formato JSON.
@RequestMapping("/api/auth") //indica ruta base para los endpoints de este controlador
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session
    ){
        return authService.login(request, session);
    }

    @GetMapping("/me")
    public LoginResponse me(HttpSession session) {
        return authService.currentUser(session);
    }

    @PostMapping("/logout")
    public void logout(HttpSession session) {
        authService.logout(session);
    }
}
