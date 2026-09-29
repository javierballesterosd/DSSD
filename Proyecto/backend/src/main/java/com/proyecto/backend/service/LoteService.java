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
import com.proyecto.backend.model.EstadoLote;

import java.time.LocalDateTime;
import java.util.Optional;

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

        Emergencia emergencia = emergenciaRepository.findById(emergenciaId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Emergencia no encontrada con ID: " + emergenciaId
                        )
                );

        Optional<Lote> ultimoLote =
                loteRepository.findFirstByEmergenciaIdOrderByIdDesc(emergenciaId);

        if (ultimoLote.isPresent()
                && ultimoLote.get().getEstado() != EstadoLote.CANCELADO) {

            throw new IllegalStateException(
                    "La emergencia ya tiene un lote que no está cancelado."
            );
        }

        Lote lote = loteMapper.toEntity(requestDTO, emergencia);

        for (var itemDTO : requestDTO.getItems()) {

            Recurso recurso = recursoRepository.findById(itemDTO.getRecursoId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Recurso no encontrado con ID: "
                                            + itemDTO.getRecursoId()
                            )
                    );

            ItemLote item = itemLoteMapper.toEntity(itemDTO, recurso);

            lote.agregarItem(item);
        }

        Lote loteGuardado = loteRepository.save(lote);

        return loteMapper.toDto(loteGuardado);
    }
}