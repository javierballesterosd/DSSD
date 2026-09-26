package com.proyecto.backend.dto.lote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LoteRequest {

    @NotBlank
    private String titulo;

    private LocalDate fechaInicio;

    @NotEmpty
    @Valid
    private List<ItemLoteRequest> items;
}