package com.proyecto.backend.dto.lote;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ItemLoteRequestDTO {

    @NotNull
    private Long recursoId;

    @NotNull
    @Positive
    private Integer cantidadRequerida;
}