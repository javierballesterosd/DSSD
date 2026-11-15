package com.proyecto.backend.service;

import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.model.Actividad;
import com.proyecto.backend.repository.ActividadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ActividadService {

    private final ActividadRepository actividadRepository;

    /** Marca la actividad como finalizada cambiando su estado a verdadero. */
    @Transactional
    public Actividad marcarComoFinalizada(Long id) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Actividad no encontrada para finalizar. id={}", id);
                    return new RecursoNoEncontradoException(
                            "No hay una actividad registrada con el id " + id);
                });

        actividad.setFinalizada(true); 
        
        log.info("La actividad con id={} fue marcada como finalizada", id);
        
        return actividadRepository.save(actividad);
    }

}
