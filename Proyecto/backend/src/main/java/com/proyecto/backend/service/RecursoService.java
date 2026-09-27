package com.proyecto.backend.service;

import com.proyecto.backend.model.Recurso;
import com.proyecto.backend.repository.RecursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecursoService {

    private final RecursoRepository recursoRepository;

    @Transactional(readOnly = true)
    public List<Recurso> obtenerRecursos() {
        return recursoRepository.findAll();
    }
}