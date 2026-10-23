package com.proyecto.backend.controller;

import com.proyecto.backend.exception.UnauthenticatedException;
import com.proyecto.backend.security.BonitaTokenValidator;
import com.proyecto.backend.service.ValidacionNacionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergencias/{id}/validacion-sn")
@RequiredArgsConstructor
public class ValidacionNacionalController {
    private final ValidacionNacionalService validaciones;
    private final BonitaTokenValidator tokens;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validar(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!tokens.isValid(authorization)) throw new UnauthenticatedException("Token de Bonita inválido o ausente");
        validaciones.validar(id);
    }
}
