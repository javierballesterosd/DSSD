package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.emergencia.EmergenciaRequestDTO;
import com.proyecto.backend.dto.emergencia.EmergenciaResponseDTO;
import com.proyecto.backend.dto.notificacion.DescriptorAudiencia;
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

@Service
@RequiredArgsConstructor
public class EmergenciaService {

    static final String TAREA_REGISTRAR_EMERGENCIA = "Registrar emergencia";

    private final EmergenciaRepository emergenciaRepository;
    private final MunicipioRepository municipioRepository;
    private final BonitaClient bonitaClient;
    private final EmergenciaMapper emergenciaMapper;
    private final NotificacionService notificacionService;

    /**
     * Guarda la emergencia del municipio del usuario, inicia el caso en Bonita y completa la tarea
     * "Registrar emergencia" con sus datos. Si Bonita falla, se revierte todo.
     */
    @Transactional
    public EmergenciaResponseDTO registrarEmergencia(EmergenciaRequestDTO requestDTO, Long municipioId,
                                                     BonitaSession session,
                                                     LoginResponse remitente) {
        Municipio municipio = municipioRepository.findById(municipioId)
                .orElseThrow(() -> new ResourceNotFoundException("Municipio no encontrado con ID: " + municipioId));

        Emergencia emergencia = emergenciaMapper.toEntity(requestDTO, municipio);
        emergencia.setFechaRegistro(LocalDateTime.now());
        Emergencia guardada = emergenciaRepository.save(emergencia);

        String processId = bonitaClient.buscarProcesoId(session, "Sistema P1");

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

        crearNotificacionEmergencia(guardada, municipio, remitente);

        return emergenciaMapper.toDto(emergenciaRepository.save(guardada));
    }

    private void crearNotificacionEmergencia(
            Emergencia emergencia,
            Municipio municipio,
            LoginResponse remitente
    ) {
        String titulo = "Nueva emergencia registrada";
        String descripcion = String.format("Se ha registrado una nueva emergencia en el municipio de %s. Nivel de gravedad: %s. Zona afectada: %s.",
                municipio.getNombre(), emergencia.getNivelGravedad().name(), emergencia.getZonaAfectada());

        notificacionService.crear(
                "COORDINADOR",
                new DescriptorAudiencia(municipio.getRegion().getBonitaGroupPath()),
                titulo,
                descripcion,
                remitente
        );
    }
}
