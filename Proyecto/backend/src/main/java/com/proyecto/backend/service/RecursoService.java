package com.proyecto.backend.service;

import com.proyecto.backend.dto.RecursoResponse;
import com.proyecto.backend.mapper.RecursoMapper;
import com.proyecto.backend.repository.RecursoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final RecursoMapper recursoMapper;

    @Transactional(readOnly = true)
    public List<RecursoResponse> obtenerRecursos() {
        List<RecursoResponse> recursos = recursoRepository.findAll().stream()
                .map(recursoMapper::toResponse)
                .toList();
        log.debug("Recursos consultados. cantidad={}", recursos.size());
        return recursos;
    }
}
