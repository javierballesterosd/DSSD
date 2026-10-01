package com.proyecto.backend.service;

import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.model.Region;
import com.proyecto.backend.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RegionService {

    private final RegionRepository regionRepository;


    public Region obtenerPorGrupoBonita(String path) {
        return regionRepository.findByBonitaGroupPath(path)
                .orElseThrow(() -> {
                    log.warn("Region no encontrada para el grupo de Bonita. path={}", path);
                    return new RecursoNoEncontradoException(
                            "No hay una región registrada para el grupo de Bonita " + path);
                });
    }

    public Region obtenerPorGrupoBonitaEnJerarquia(String path) {
        String grupo = path;
        while (grupo != null && !grupo.isBlank()) {
            Optional<Region> region = regionRepository.findByBonitaGroupPath(grupo);
            if (region.isPresent()) {
                return region.get();
            }
            grupo = grupoPadre(grupo);
        }
        log.warn("Region no encontrada en la jerarquía del grupo de Bonita. path={}", path);
        throw new RecursoNoEncontradoException(
                "No hay una región registrada en la jerarquía del grupo de Bonita " + path);
    }

    private String grupoPadre(String path) {
        int ultimoSeparador = path.lastIndexOf('/');
        return ultimoSeparador <= 0 ? null : path.substring(0, ultimoSeparador);
    }

}
