package com.proyecto.backend.dto;

import java.util.*;

public record ValidacionSNRequest(
    boolean aprobada,
    String observaciones,
    List<ItemValidadoRequest> itemsValidados
) {
}

