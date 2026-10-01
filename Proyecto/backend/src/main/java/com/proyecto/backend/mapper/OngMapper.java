package com.proyecto.backend.mapper;

import com.proyecto.backend.dto.DisponibilidadRecursoResponse;
import com.proyecto.backend.dto.InventarioOngResponse;
import com.proyecto.backend.dto.OngResponse;
import com.proyecto.backend.model.InventarioOng;
import com.proyecto.backend.model.Ong;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OngMapper {

    public OngResponse toResponse(Ong ong) {
        return new OngResponse(ong.getId(), ong.getRazonSocial());
    }

    public InventarioOngResponse toInventarioResponse(Ong ong, List<InventarioOng> inventario) {
        List<DisponibilidadRecursoResponse> recursos = inventario.stream()
                .map(this::toDisponibilidadResponse)
                .toList();
        return new InventarioOngResponse(ong.getId(), ong.getRazonSocial(), recursos);
    }

    private DisponibilidadRecursoResponse toDisponibilidadResponse(InventarioOng inventario) {
        return new DisponibilidadRecursoResponse(
                inventario.getRecurso().getId(),
                inventario.getRecurso().getNombre(),
                inventario.getRecurso().getUnidadMedida(),
                inventario.getCantidadDisponible()
        );
    }
}
