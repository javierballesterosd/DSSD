package com.proyecto.backend.service;

import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.repository.MunicipioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MunicipioService {

    private final MunicipioRepository municipioRepository;

    public MunicipioService(MunicipioRepository municipioRepository) {
        this.municipioRepository = municipioRepository;
    }

    /** Devuelve el municipio con su región cargada (para usarlo fuera de la transacción). */
    public Municipio obtenerPorGrupoBonita(String path) {
        return municipioRepository.findByBonitaGroupPath(path)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay un municipio registrado para el grupo de Bonita " + path));
    }
}
