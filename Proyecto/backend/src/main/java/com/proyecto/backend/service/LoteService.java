package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.lote.LoteRequestDTO;
import com.proyecto.backend.dto.lote.LoteResponseDTO;
import com.proyecto.backend.exception.BonitaIntegrationException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.exception.ResourceNotFoundException;
import com.proyecto.backend.mapper.ItemLoteMapper;
import com.proyecto.backend.dto.LoteDetalleResponse;
import com.proyecto.backend.dto.LoteResumenResponse;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.mapper.LoteMapper;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.model.Recurso;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.RecursoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import com.proyecto.backend.model.EstadoLote;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoteService {

    static final String TAREA_DESGLOSAR_LOTES = "Desglosar en \"Lotes Necesidades\"";

    private final LoteRepository loteRepository;
    private final EmergenciaRepository emergenciaRepository;
    private final RecursoRepository recursoRepository;
    private final LoteMapper loteMapper;
    private final ItemLoteMapper itemLoteMapper;
    private final BonitaClient bonitaClient;

    @Transactional
    public LoteResponseDTO publicarLote(
            Long emergenciaId,
            LoteRequestDTO requestDTO,
            BonitaSession session) {

        if (!requestDTO.getFechaCierreOfertas().isAfter(requestDTO.getFechaAperturaOfertas())) {
            throw new ReglaNegocioException(
                    "La fecha de cierre de ofertas debe ser posterior a la de apertura."
            );
        }

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

        completarDesgloseEnBonita(emergencia, loteGuardado, session);

        return loteMapper.toDto(loteGuardado);
    }

    /**
     * Completa la tarea "Desglosar" del caso de la emergencia con el lote y su ventana de
     * ofertas. Si Bonita falla se propaga la excepción y se revierte el alta del lote.
     */
    private void completarDesgloseEnBonita(
            Emergencia emergencia,
            Lote lote,
            BonitaSession session) {

        String caseId = emergencia.getBonitaCaseId();

        if (caseId == null) {
            throw new ReglaNegocioException(
                    "La emergencia no tiene un caso asociado en Bonita."
            );
        }

        try {
            Map<String, Object> contrato = new LinkedHashMap<>();
            contrato.put("loteId", lote.getId());
            contrato.put("fechaAperturaOfertas", formatoBonita(lote.getFechaAperturaOfertas()));
            contrato.put("fechaCierreOfertas", formatoBonita(lote.getFechaCierreOfertas()));

            String tareaId = bonitaClient.buscarTareaPendiente(
                    session, caseId, TAREA_DESGLOSAR_LOTES
            );

            bonitaClient.ejecutarTarea(session, tareaId, contrato);

            log.info(
                    "Tarea de desglose completada. loteId={}, caseId={}, tareaId={}",
                    lote.getId(), caseId, tareaId
            );

        } catch (RestClientException e) {
            log.error(
                    "Error al completar el desglose en Bonita. loteId={}, caseId={}",
                    lote.getId(), caseId, e
            );

            throw new BonitaIntegrationException(
                    "No se pudo completar la tarea de desglose en Bonita (caso " + caseId + ")",
                    e
            );
        }
    }

    // Bonita parsea LocalDateTime como ISO con segundos (yyyy-MM-ddTHH:mm:ss)
    private String formatoBonita(LocalDateTime fecha) {
        return fecha.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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

