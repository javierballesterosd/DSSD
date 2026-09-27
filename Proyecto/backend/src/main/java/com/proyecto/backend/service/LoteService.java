package com.proyecto.backend.service;

import com.proyecto.backend.dto.LoteDetalleResponse;
import com.proyecto.backend.dto.LoteResumenResponse;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.mapper.LoteMapper;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.repository.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LoteService {

    private final LoteRepository loteRepository;
    private final LoteMapper loteMapper;

    public LoteService(LoteRepository loteRepository, LoteMapper loteMapper) {
        this.loteRepository = loteRepository;
        this.loteMapper = loteMapper;
    }

    public List<LoteResumenResponse> listarPublicados(EstadoLote estado) {
        return loteRepository.findByEstado(estado).stream()
                .map(loteMapper::toResumenResponse)
                .toList();
    }

    public LoteDetalleResponse obtenerDetalle(Long id) {
        Lote lote = loteRepository.findDetalleById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el lote " + id));
        return loteMapper.toDetalleResponse(lote);
    }
}
