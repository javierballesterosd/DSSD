package com.proyecto.backend.controller;

import com.proyecto.backend.dto.lote.LoteRequestDTO;
import com.proyecto.backend.dto.lote.LoteResponseDTO;
import com.proyecto.backend.service.LoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergencias/{emergenciaId}/lotes")
@RequiredArgsConstructor
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
}