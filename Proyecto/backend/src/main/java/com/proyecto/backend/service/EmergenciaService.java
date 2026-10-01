package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.EmergenciaParaLoteResponse;
import com.proyecto.backend.dto.EmergenciaRequest;
import com.proyecto.backend.dto.EmergenciaResponse;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.mapper.EmergenciaMapper;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergenciaService {

    static final String TAREA_REGISTRAR_EMERGENCIA = "Registrar emergencia";

    private final EmergenciaRepository emergenciaRepository;
    private final MunicipioRepository municipioRepository;
    private final BonitaClient bonitaClient;
    private final EmergenciaMapper emergenciaMapper;
    private final LoteRepository loteRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Registra una emergencia: primero la guarda en la base (dentro de la transacción) y después, como
     * último paso, inicia el caso en Bonita y completa la tarea. Si Bonita falla se revierte todo: la
     * transacción hace rollback y se elimina el caso de Bonita si llegó a crearse. La notificación a
     * los coordinadores se publica como evento y se crea recién después del commit.
     */
    @Transactional
    public EmergenciaResponse registrarEmergencia(
            EmergenciaRequest request,
            Long municipioId,
            LoginResponse remitente,
            BonitaSession session) {

        log.info("Registrando emergencia. municipioId={}, usuario={}",
                municipioId, remitente.getUsername());

        Municipio municipio = municipioRepository.findById(municipioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Municipio no encontrado con ID: " + municipioId));

        Emergencia emergencia = emergenciaMapper.toEntity(request, municipio);
        emergencia.setFechaRegistro(LocalDateTime.now());
        Emergencia guardada = emergenciaRepository.save(emergencia);
        log.info("Emergencia guardada. emergenciaId={}", guardada.getId());

        String caseId = null;
        try {
            String processId = bonitaClient.buscarProcesoId(session);
            caseId = bonitaClient.iniciarCaso(session, processId);

            Map<String, Object> contrato = new LinkedHashMap<>();
            contrato.put("emergenciaId", guardada.getId());
            contrato.put("municipioId", municipio.getId());
            contrato.put("nivelGravedad", guardada.getNivelGravedad().name());
            contrato.put("zonaAfectada", guardada.getZonaAfectada());
            contrato.put("descripcion", guardada.getDescripcion());
            contrato.put("regionGroupPath", municipio.getRegion().getBonitaGroupPath());

            String tareaId = bonitaClient.buscarTareaPendiente(
                    session, caseId, TAREA_REGISTRAR_EMERGENCIA);
            bonitaClient.ejecutarTarea(session, tareaId, contrato);

            guardada.setBonitaCaseId(caseId);
            guardada = emergenciaRepository.save(guardada);
        } catch (RuntimeException e) {
            log.error("Falló el registro en Bonita; se revierte la emergencia. emergenciaId={}, caseId={}",
                    guardada.getId(), caseId, e);
            compensarCaso(session, caseId);
            throw e;
        }

        log.info("Emergencia registrada. emergenciaId={}, municipioId={}, caseId={}",
                guardada.getId(), municipioId, caseId);

        eventPublisher.publishEvent(new EmergenciaRegistradaEvent(
                guardada.getId(),
                municipio.getNombre(),
                guardada.getNivelGravedad().name(),
                guardada.getZonaAfectada(),
                municipio.getRegion().getBonitaGroupPath(),
                remitente));

        return emergenciaMapper.toResponse(guardada);
    }

    /** Elimina el caso de Bonita si ya se creó; si no se puede, queda el aviso para limpieza manual. */
    private void compensarCaso(BonitaSession session, String caseId) {
        if (caseId == null) {
            return;
        }
        try {
            bonitaClient.cancelarCaso(session, caseId);
        } catch (RuntimeException e) {
            log.error("No se pudo eliminar el caso huérfano en Bonita; requiere limpieza manual. caseId={}",
                    caseId, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<EmergenciaParaLoteResponse> obtenerEmergenciasParaLotes(Long regionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return emergenciaRepository
                .findEmergenciasParaLotes(regionId, pageable)
                .map(this::conUltimoLote);
    }

    /** Emergencias registradas por un municipio, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<EmergenciaParaLoteResponse> listarDeMunicipio(Long municipioId) {
        return emergenciaRepository.findByMunicipioIdOrderByFechaRegistroDesc(municipioId).stream()
                .map(this::conUltimoLote)
                .toList();
    }

    /**
     * Detalle de una emergencia. El operador municipal solo ve las de su municipio y el coordinador
     * las de su región; el auditor ve todas.
     */
    @Transactional(readOnly = true)
    public EmergenciaParaLoteResponse obtenerDetalle(Long id, LoginResponse usuario) {
        Emergencia emergencia = emergenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la emergencia " + id));

        Municipio municipio = emergencia.getMunicipio();
        boolean permitido = switch (usuario.getRole()) {
            case "MUNICIPAL" -> municipio.getId().equals(usuario.getMunicipioId());
            case "COORDINADOR" -> municipio.getRegion().getId().equals(usuario.getRegionId());
            case "AUDITOR" -> true;
            default -> false;
        };
        if (!permitido) {
            throw new AccesoDenegadoException("No tenés permiso para ver esta emergencia");
        }
        return conUltimoLote(emergencia);
    }

    private EmergenciaParaLoteResponse conUltimoLote(Emergencia emergencia) {
        return emergenciaMapper.toParaLoteResponse(
                emergencia,
                loteRepository.findFirstByEmergenciaIdOrderByIdDesc(emergencia.getId()));
    }
}
