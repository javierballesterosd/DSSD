package com.proyecto.backend.dto.lote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LoteRequestDTO {

    @NotBlank
    private String titulo;

    @NotEmpty
    @Valid
    private List<ItemLoteRequestDTO> items;
}