package com.proyecto.backend.service;

import com.proyecto.backend.dto.lote.LoteRequestDTO;
import com.proyecto.backend.dto.lote.LoteResponseDTO;
import com.proyecto.backend.exception.ResourceNotFoundException;
import com.proyecto.backend.mapper.ItemLoteMapper;
import com.proyecto.backend.mapper.LoteMapper;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.model.Recurso;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.RecursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository loteRepository;
    private final EmergenciaRepository emergenciaRepository;
    private final RecursoRepository recursoRepository;
    private final LoteMapper loteMapper;
    private final ItemLoteMapper itemLoteMapper;

    @Transactional
    public LoteResponseDTO publicarLote(Long emergenciaId, LoteRequestDTO requestDTO) {

        // 1. Obtener emergencia
        Emergencia emergencia = emergenciaRepository.findById(emergenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergencia no encontrada con ID: " + emergenciaId));

        // 2. Mapear lote
        Lote lote = loteMapper.toEntity(requestDTO, emergencia);

        // 3. Obtener recursos y asociar ítems
        for (var itemDTO : requestDTO.getItems()) {

            Recurso recurso = recursoRepository.findById(itemDTO.getRecursoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado con ID: " + itemDTO.getRecursoId()));

            ItemLote item = itemLoteMapper.toEntity(itemDTO, recurso);

            lote.agregarItem(item);
        }

        // 4. Guardar lote
        Lote loteGuardado = loteRepository.save(lote);

        // 5. Mapear respuesta
        return loteMapper.toDto(loteGuardado);
    }
}