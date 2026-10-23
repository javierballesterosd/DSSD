package com.proyecto.backend.dto;

import java.util.*;

public record ValidacionSNRequest(
    @jakarta.validation.constraints.NotNull Boolean aprobada,
    String observaciones,
    List<ItemValidadoRequest> itemsValidados
) {
}

