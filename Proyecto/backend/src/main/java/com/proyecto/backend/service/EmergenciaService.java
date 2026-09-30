package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.emergencia.EmergenciaRequestDTO;
import com.proyecto.backend.dto.emergencia.EmergenciaResponseDTO;
import com.proyecto.backend.exception.ResourceNotFoundException;
import com.proyecto.backend.mapper.EmergenciaMapper;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import com.proyecto.backend.dto.lote.EmergenciaLoteResponseDTO;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.repository.LoteRepository;
import java.util.Optional;
import java.time.LocalDateTime;import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class EmergenciaService {

    static final String TAREA_REGISTRAR_EMERGENCIA = "Registrar emergencia";

    private final EmergenciaRepository emergenciaRepository;
    private final MunicipioRepository municipioRepository;
    private final BonitaClient bonitaClient;
    private final EmergenciaMapper emergenciaMapper;
    private final LoteRepository loteRepository;

    /**
     * Guarda la emergencia del municipio del usuario, inicia el caso en Bonita y completa la tarea
     * "Registrar emergencia" con sus datos. Si Bonita falla, se revierte todo.
     */
    @Transactional
    public EmergenciaResponseDTO registrarEmergencia(EmergenciaRequestDTO requestDTO, Long municipioId,
                                                     BonitaSession session) {
        Municipio municipio = municipioRepository.findById(municipioId)
                .orElseThrow(() -> new ResourceNotFoundException("Municipio no encontrado con ID: " + municipioId));

        Emergencia emergencia = emergenciaMapper.toEntity(requestDTO, municipio);
        emergencia.setFechaRegistro(LocalDateTime.now());
        Emergencia guardada = emergenciaRepository.save(emergencia);

        String processId = bonitaClient.buscarProcesoId(session, "Sistema");

        String caseId = bonitaClient.iniciarCaso(
                session,
                processId
        );

        Map<String, Object> contrato = new LinkedHashMap<>();
        contrato.put("emergenciaId", guardada.getId());
        contrato.put("municipioId", municipio.getId());
        contrato.put("nivelGravedad", guardada.getNivelGravedad().name());
        contrato.put("zonaAfectada", guardada.getZonaAfectada());
        contrato.put("descripcion", guardada.getDescripcion());
        // Path de la región, para que la app filtre las tareas del CCR por región
        contrato.put("regionGroupPath", municipio.getRegion().getBonitaGroupPath());
        String tareaId = bonitaClient.buscarTareaPendiente(session, caseId, TAREA_REGISTRAR_EMERGENCIA);
        bonitaClient.ejecutarTarea(session, tareaId, contrato);

        guardada.setBonitaCaseId(caseId);
        return emergenciaMapper.toDto(emergenciaRepository.save(guardada));
    }

    @Transactional(readOnly = true)
    public Page<EmergenciaLoteResponseDTO> obtenerEmergenciasParaLotes(int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size
        );

        return emergenciaRepository
                .findEmergenciasParaLotes(pageable)
                .map(emergencia -> {

                    Optional<Lote> ultimoLote =
                            loteRepository.findFirstByEmergenciaIdOrderByIdDesc(
                                    emergencia.getId()
                            );

                    return new EmergenciaLoteResponseDTO(
                            emergencia.getId(),
                            emergencia.getDescripcion(),
                            emergencia.getNivelGravedad(),
                            emergencia.getZonaAfectada(),
                            emergencia.getMunicipio().getId(),
                            emergencia.getFechaRegistro(),
                            ultimoLote.map(Lote::getId).orElse(null),
                            ultimoLote.map(Lote::getEstado).orElse(null)
                    );
                });
    }
}