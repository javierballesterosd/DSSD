package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.LoteRequest;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.mapper.ItemLoteMapper;
import com.proyecto.backend.dto.LoteDetalleResponse;
import com.proyecto.backend.dto.LoteResumenResponse;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publica el lote de una emergencia de la región del coordinador. La notificación al municipio
     * se publica como evento y se crea recién después del commit.
     */
    @Transactional
    public LoteDetalleResponse publicarLote(
            Long emergenciaId,
            LoteRequest request,
            LoginResponse coordinador,
            BonitaSession session) {

        if (!request.fechaCierreOfertas().isAfter(request.fechaAperturaOfertas())) {
            throw new ReglaNegocioException(
                    "La fecha de cierre de ofertas debe ser posterior a la de apertura."
            );
        }

        // Se compara contra el minuto actual: el formulario no manda segundos
        if (request.fechaAperturaOfertas().isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))) {
            throw new ReglaNegocioException(
                    "La fecha de apertura de ofertas no puede ser anterior a la fecha y hora actual."
            );
        }

        // Cada recurso va una sola vez en el lote
        Set<Long> recursoIds = new HashSet<>();
        for (var itemRequest : request.items()) {
            if (!recursoIds.add(itemRequest.recursoId())) {
                throw new ReglaNegocioException(
                        "El lote no puede tener el mismo recurso más de una vez."
                );
            }
        }

        Emergencia emergencia = emergenciaRepository.findById(emergenciaId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Emergencia no encontrada con ID: " + emergenciaId
                        )
                );

        if (!emergencia.getMunicipio().getRegion().getId().equals(coordinador.getRegionId())) {
            log.warn("Publicación rechazada: la emergencia es de otra región. emergenciaId={}, regionId={}",
                    emergenciaId, coordinador.getRegionId());
            throw new AccesoDenegadoException("La emergencia no pertenece a tu región");
        }

        Optional<Lote> ultimoLote =
                loteRepository.findFirstByEmergenciaIdOrderByIdDesc(emergenciaId);

        if (ultimoLote.isPresent()
                && ultimoLote.get().getEstado() != EstadoLote.CANCELADO) {

            log.warn("Publicación rechazada: la emergencia ya tiene un lote activo. emergenciaId={}, loteId={}",
                    emergenciaId, ultimoLote.get().getId());
            throw new ReglaNegocioException(
                    "La emergencia ya tiene un lote que no está cancelado."
            );
        }

        Lote lote = loteMapper.toEntity(request, emergencia);

        for (var itemRequest : request.items()) {

            Recurso recurso = recursoRepository.findById(itemRequest.recursoId())
                    .orElseThrow(() ->
                            new RecursoNoEncontradoException(
                                    "Recurso no encontrado con ID: "
                                            + itemRequest.recursoId()
                            )
                    );

            ItemLote item = itemLoteMapper.toEntity(itemRequest, recurso);

            lote.agregarItem(item);
        }

        Lote loteGuardado = loteRepository.save(lote);
        log.info("Lote guardado. loteId={}, emergenciaId={}, items={}",
                loteGuardado.getId(), emergenciaId, loteGuardado.getItems().size());

        // Último paso: si Bonita falla, la excepción revierte el alta del lote
        completarDesgloseEnBonita(emergencia, loteGuardado, session);

        eventPublisher.publishEvent(new LoteCambiadoEvent(
                loteGuardado.getId(),
                loteGuardado.getTitulo(),
                loteGuardado.getEstado(),
                loteGuardado.getFechaAperturaOfertas(),
                loteGuardado.getFechaCierreOfertas(),
                emergencia.getId(),
                emergencia.getZonaAfectada(),
                emergencia.getMunicipio().getBonitaGroupPath(),
                coordinador));

        return loteMapper.toDetalleResponse(loteGuardado);
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
            log.warn("La emergencia no tiene caso en Bonita. emergenciaId={}", emergencia.getId());
            throw new ReglaNegocioException(
                    "La emergencia no tiene un caso asociado en Bonita."
            );
        }

        Map<String, Object> contrato = new LinkedHashMap<>();
        contrato.put("loteId", lote.getId());
        contrato.put("fechaAperturaOfertas", formatoBonita(lote.getFechaAperturaOfertas()));
        contrato.put("fechaCierreOfertas", formatoBonita(lote.getFechaCierreOfertas()));

        String tareaId = bonitaClient.buscarTareaPendiente(session, caseId, TAREA_DESGLOSAR_LOTES);
        bonitaClient.ejecutarTarea(session, tareaId, contrato);

        log.info("Tarea de desglose completada. loteId={}, caseId={}, tareaId={}",
                lote.getId(), caseId, tareaId);
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

    /** Lotes de las emergencias de una región, en cualquier estado, del más reciente al más antiguo. */
    public List<LoteResumenResponse> listarDeRegion(Long regionId) {
        return loteRepository.findByEmergenciaMunicipioRegionIdOrderByFechaCreacionDesc(regionId).stream()
                .map(loteMapper::toResumenResponse)
                .toList();
    }

    /** Detalle de un lote. El coordinador solo ve los de su región. */
    @Transactional(readOnly = true)
    public LoteDetalleResponse obtenerDetalle(Long id, LoginResponse usuario) {
        Lote lote = loteRepository.findDetalleById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el lote " + id));

        if ("COORDINADOR".equals(usuario.getRole())
                && !lote.getEmergencia().getMunicipio().getRegion().getId().equals(usuario.getRegionId())) {
            throw new AccesoDenegadoException("El lote no pertenece a tu región");
        }
        return loteMapper.toDetalleResponse(lote);
    }
}

