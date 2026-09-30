package com.proyecto.backend.service;

import com.proyecto.backend.dto.InventarioOngResponse;
import com.proyecto.backend.dto.OngResponse;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.mapper.OngMapper;
import com.proyecto.backend.model.InventarioOng;
import com.proyecto.backend.model.Ong;
import com.proyecto.backend.repository.InventarioOngRepository;
import com.proyecto.backend.repository.OngRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class OngService {

    private final OngRepository ongRepository;
    private final InventarioOngRepository inventarioOngRepository;
    private final OngMapper ongMapper;

    public OngService(OngRepository ongRepository, InventarioOngRepository inventarioOngRepository,
                       OngMapper ongMapper) {
        this.ongRepository = ongRepository;
        this.inventarioOngRepository = inventarioOngRepository;
        this.ongMapper = ongMapper;
    }

    public List<OngResponse> listar() {
        return ongRepository.findAllByOrderByRazonSocialAsc().stream()
                .map(ongMapper::toResponse)
                .toList();
    }

    public Ong obtenerPorGrupoBonita(String path) {
        return ongRepository.findByBonitaGroupPath(path)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay una ONG registrada para el grupo de Bonita " + path));
    }

    public List<InventarioOngResponse> inventarioPorOng(Set<Long> ongIds) {
        List<Ong> ongs = ongRepository.findAllById(ongIds);
        if (ongs.size() != ongIds.size()) {
            throw new RecursoNoEncontradoException("Alguna de las ONGs solicitadas no existe");
        }

        Map<Long, List<InventarioOng>> inventarioPorOngId = new LinkedHashMap<>();
        for (InventarioOng inventario : inventarioOngRepository.findByOngIdIn(ongIds)) {
            inventarioPorOngId
                    .computeIfAbsent(inventario.getOng().getId(), id -> new java.util.ArrayList<>())
                    .add(inventario);
        }

        return ongs.stream()
                .sorted((a, b) -> a.getRazonSocial().compareToIgnoreCase(b.getRazonSocial()))
                .map(ong -> ongMapper.toInventarioResponse(
                        ong, inventarioPorOngId.getOrDefault(ong.getId(), List.of())))
                .toList();
    }
}
