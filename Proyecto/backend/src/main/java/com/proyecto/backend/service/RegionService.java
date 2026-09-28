package com.proyecto.backend.service;

import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.model.Region;
import com.proyecto.backend.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    public Region obtenerPorGrupoBonita(String path) {
        return regionRepository.findByBonitaGroupPath(path)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay una región registrada para el grupo de Bonita " + path));
    }
}
