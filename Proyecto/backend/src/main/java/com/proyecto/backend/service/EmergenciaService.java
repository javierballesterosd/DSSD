
package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.emergencia.EmergenciaRequestDTO;
import com.proyecto.backend.dto.emergencia.EmergenciaResponseDTO;
import com.proyecto.backend.dto.notificacion.DescriptorAudiencia;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.BonitaIntegrationException;
import com.proyecto.backend.exception.InvalidCredentialsException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.exception.ResourceNotFoundException;
import com.proyecto.backend.exception.UnauthenticatedException;
import com.proyecto.backend.mapper.EmergenciaMapper;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
@Slf4j
public class EmergenciaService {

    static final String TAREA_REGISTRAR_EMERGENCIA = "Registrar emergencia";

    private final EmergenciaRepository emergenciaRepository;
    private final MunicipioRepository municipioRepository;
    private final BonitaClient bonitaClient;
    private final EmergenciaMapper emergenciaMapper;
    private final NotificacionService notificacionService;
    private final LoteRepository loteRepository;

    /**
     * Registra una emergencia, inicia el caso en Bonita
     * y ejecuta la tarea correspondiente.
     */
    @Transactional
    public EmergenciaResponseDTO registrarEmergencia(
            EmergenciaRequestDTO requestDTO,
            Long municipioId,
            LoginResponse remitente,
            BonitaSession session) {

        String etapa = "Inicio del registro";
        Long emergenciaId = null;
        String caseId = null;

        log.info("Iniciando registro de emergencia. municipioId={}", municipioId);

        try {
            etapa = "Buscar municipio";

            Municipio municipio = municipioRepository.findById(municipioId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Municipio no encontrado con ID: " + municipioId));

            log.info("Municipio encontrado. municipioId={}", municipioId);

            etapa = "Guardar emergencia en base de datos";

            Emergencia emergencia = emergenciaMapper.toEntity(requestDTO, municipio);
            emergencia.setFechaRegistro(LocalDateTime.now());

            Emergencia guardada = emergenciaRepository.save(emergencia);
            emergenciaId = guardada.getId();

            log.info(
                    "Emergencia guardada. emergenciaId={}, municipioId={}",
                    emergenciaId, municipioId
            );

            etapa = "Buscar proceso en Bonita";

            log.info("Buscando proceso en Bonita. nombre=Sistema P1");

            String processId = bonitaClient.buscarProcesoId(session, "Sistema P1");

            log.info("Proceso encontrado en Bonita. processId={}", processId);

            etapa = "Iniciar caso en Bonita";

            log.info("Iniciando caso en Bonita. processId={}", processId);

            caseId = bonitaClient.iniciarCaso(session, processId);

            log.info(
                    "Caso iniciado en Bonita. caseId={}, emergenciaId={}",
                    caseId, emergenciaId
            );

            etapa = "Preparar contrato";

            Map<String, Object> contrato = new LinkedHashMap<>();
            contrato.put("emergenciaId", guardada.getId());
            contrato.put("municipioId", municipio.getId());
            contrato.put("nivelGravedad", guardada.getNivelGravedad().name());
            contrato.put("zonaAfectada", guardada.getZonaAfectada());
            contrato.put("descripcion", guardada.getDescripcion());
            contrato.put("regionGroupPath", municipio.getRegion().getBonitaGroupPath());

            etapa = "Buscar tarea pendiente en Bonita";

            log.info(
                    "Buscando tarea en Bonita. caseId={}, tarea={}",
                    caseId, TAREA_REGISTRAR_EMERGENCIA
            );

            String tareaId = bonitaClient.buscarTareaPendiente(
                    session, caseId, TAREA_REGISTRAR_EMERGENCIA
            );

            log.info(
                    "Tarea encontrada. tareaId={}, caseId={}",
                    tareaId, caseId
            );

            etapa = "Ejecutar tarea en Bonita";

            log.info(
                    "Ejecutando tarea en Bonita. tareaId={}, caseId={}",
                    tareaId, caseId
            );

            bonitaClient.ejecutarTarea(session, tareaId, contrato);

            log.info(
                    "Tarea ejecutada correctamente. tareaId={}, caseId={}",
                    tareaId, caseId
            );

            etapa = "Guardar identificador del caso";

            guardada.setBonitaCaseId(caseId);
            Emergencia finalizada = emergenciaRepository.save(guardada);

            log.info(
                    "Registro de emergencia completado. emergenciaId={}, " +
                            "municipioId={}, caseId={}",
                    emergenciaId, municipioId, caseId
            );

            crearNotificacionEmergencia(guardada, municipio, remitente);

            return emergenciaMapper.toDto(finalizada);

        } catch (ResourceNotFoundException e) {
            log.warn(
                    "Recurso no encontrado. etapa={}, municipioId={}, mensaje={}",
                    etapa, municipioId, e.getMessage()
            );
            throw e;

        } catch (ResourceAccessException e) {
            log.error(
                    "No se pudo conectar con Bonita. etapa={}, " +
                            "emergenciaId={}, municipioId={}",
                    etapa, emergenciaId, municipioId, e
            );

            throw new BonitaIntegrationException(
                    "No se pudo establecer la conexión con Bonita durante la etapa: "
                            + etapa,
                    e
            );

        } catch (RestClientResponseException e) {
            log.error(
                    "Bonita devolvió un error HTTP. etapa={}, emergenciaId={}, " +
                            "municipioId={}, status={}",
                    etapa, emergenciaId, municipioId,
                    e.getStatusCode().value(), e
            );

            throw new BonitaIntegrationException(
                    "Bonita devolvió el estado HTTP "
                            + e.getStatusCode().value()
                            + " durante la etapa: " + etapa,
                    e
            );

        } catch (BonitaIntegrationException e) {
            log.error(
                    "Error de integración con Bonita. etapa={}, " +
                            "emergenciaId={}, municipioId={}, mensaje={}",
                    etapa, emergenciaId, municipioId, e.getMessage(), e
            );
            throw e;

        } catch (ReglaNegocioException | AccesoDenegadoException |
                 UnauthenticatedException | InvalidCredentialsException e) {
            log.warn(
                    "Registro rechazado. etapa={}, municipioId={}, " +
                            "tipoError={}, mensaje={}",
                    etapa, municipioId,
                    e.getClass().getSimpleName(), e.getMessage()
            );
            throw e;

        } catch (RestClientException e) {
            log.error(
                    "Error en la comunicación con Bonita. etapa={}, " +
                            "emergenciaId={}, municipioId={}, tipoError={}",
                    etapa, emergenciaId, municipioId,
                    e.getClass().getSimpleName(), e
            );

            throw new BonitaIntegrationException(
                    "Error de comunicación con Bonita durante la etapa: " + etapa,
                    e
            );

        } catch (RuntimeException e) {
            log.error(
                    "Error inesperado al registrar emergencia. etapa={}, " +
                            "emergenciaId={}, municipioId={}, caseId={}, " +
                            "tipoError={}, mensaje={}",
                    etapa, emergenciaId, municipioId, caseId,
                    e.getClass().getSimpleName(), e.getMessage(), e
            );
            throw e;
        }
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