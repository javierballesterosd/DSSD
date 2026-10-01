package com.proyecto.backend.service;

import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MunicipioService {

    private final MunicipioRepository municipioRepository;


    /** Devuelve el municipio con su región cargada (para usarlo fuera de la transacción). */
    public Municipio obtenerPorGrupoBonita(String path) {
        return municipioRepository.findByBonitaGroupPath(path)
                .orElseThrow(() -> {
                    log.warn("Municipio no encontrado para el grupo de Bonita. path={}", path);
                    return new RecursoNoEncontradoException(
                            "No hay un municipio registrado para el grupo de Bonita " + path);
                });
    }

    public Municipio obtenerPorGrupoBonitaEnJerarquia(String path) {
        String grupo = path;
        while (grupo != null && !grupo.isBlank()) {
            Optional<Municipio> municipio = municipioRepository.findByBonitaGroupPath(grupo);
            if (municipio.isPresent()) {
                return municipio.get();
            }
            grupo = grupoPadre(grupo);
        }
        log.warn("Municipio no encontrado en la jerarquía del grupo de Bonita. path={}", path);
        throw new RecursoNoEncontradoException(
                "No hay un municipio registrado en la jerarquía del grupo de Bonita " + path);
    }

    private String grupoPadre(String path) {
        int ultimoSeparador = path.lastIndexOf('/');
        return ultimoSeparador <= 0 ? null : path.substring(0, ultimoSeparador);
    }

}
