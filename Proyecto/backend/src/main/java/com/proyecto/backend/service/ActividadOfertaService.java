package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.MiDespliegueResponse;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.model.ActividadOferta;
import com.proyecto.backend.model.EstadoOferta;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.Oferta;
import com.proyecto.backend.model.Ong;
import com.proyecto.backend.repository.ActividadOfertaRepository;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.OfertaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Seguimiento de las actividades por parte de las ONGs una vez adjudicada la oferta. El avance es de la oferta:
 * si una ONG de un consorcio marca una actividad, queda hecha para todas las ONGs de esa oferta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActividadOfertaService {

    private static final Set<EstadoOferta> EN_CURSO = Set.of(EstadoOferta.ADJUDICADA, EstadoOferta.EN_EJECUCION);

    private final ActividadOfertaRepository avances;
    private final OfertaRepository ofertas;
    private final EmergenciaRepository emergencias;
    private final DespliegueService despliegue;
    private final PlatformTransactionManager transactionManager;

    /** Ofertas adjudicadas en curso donde participa la ONG, con el avance de cada actividad. */
    @Transactional(readOnly = true)
    public List<MiDespliegueResponse> misDespliegues(Long ongId) {
        Map<Long, List<ActividadOferta>> porOferta = new LinkedHashMap<>();
        for (ActividadOferta avance : avances.findDeOng(ongId, EN_CURSO)) {
            porOferta.computeIfAbsent(avance.getOferta().getId(), id -> new ArrayList<>()).add(avance);
        }
        return porOferta.values().stream().map(this::aRespuesta)
                .sorted(Comparator.comparing(MiDespliegueResponse::emergenciaId).reversed()
                        .thenComparing(MiDespliegueResponse::ofertaId))
                .toList();
    }

    /**
     * Marca como hecha una actividad de la oferta. Es idempotente: repetirla no cambia nada. Al terminar avisa del
     * posible cierre del monitoreo, pero un fallo de Bonita no deshace la marca (el CCR puede reintentar).
     */
    public MiDespliegueResponse.Actividad finalizar(Long ofertaId, Long actividadId, Long ongId, String usuario,
            BonitaSession sesion) {
        Long emergenciaId = transaccion(() -> ofertas.findById(ofertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la oferta " + ofertaId))
                .getLote().getEmergencia().getId());
        MiDespliegueResponse.Actividad respuesta = transaccion(() -> {
            // Se bloquea la emergencia: dos ONGs del consorcio pueden marcar a la vez.
            Emergencia emergencia = emergencias.bloquearPorId(emergenciaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la emergencia"));
            Oferta oferta = ofertas.findById(ofertaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la oferta " + ofertaId));
            Ong ong = oferta.getOngs().stream().filter(o -> ongId != null && ongId.equals(o.getId())).findFirst()
                    .orElseThrow(() -> new AccesoDenegadoException("Tu ONG no participa en esta oferta"));
            if (!EN_CURSO.contains(oferta.getEstado()) || emergencia.getFechaFinalizacion() != null)
                throw new ReglaNegocioException("La oferta no está en ejecución");
            ActividadOferta avance = avances.findByActividadIdAndOfertaId(actividadId, ofertaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("La actividad no corresponde a esta oferta"));
            if (!avance.isFinalizada()) {
                avance.setFinalizada(true);
                avance.setFechaFinalizacion(LocalDateTime.now());
                avance.setUsuarioFinalizacion(usuario);
                avance.setOngFinalizacion(ong);
                avances.saveAndFlush(avance);
                if (oferta.getEstado() == EstadoOferta.ADJUDICADA) {
                    oferta.setEstado(EstadoOferta.EN_EJECUCION);
                    oferta.setFechaModificacion(LocalDateTime.now());
                    ofertas.save(oferta);
                }
                log.info("Actividad finalizada. ofertaId={}, actividadId={}, ongId={}, usuario={}",
                        ofertaId, actividadId, ongId, usuario);
            }
            return aActividad(avance);
        });
        try {
            despliegue.verificarCierre(emergenciaId, sesion);
        } catch (RuntimeException e) {
            log.warn("No se pudo cerrar el monitoreo tras finalizar la actividad. emergenciaId={}", emergenciaId, e);
        }
        return respuesta;
    }

    private MiDespliegueResponse aRespuesta(List<ActividadOferta> filas) {
        Oferta oferta = filas.get(0).getOferta();
        Emergencia emergencia = oferta.getLote().getEmergencia();
        List<MiDespliegueResponse.Actividad> actividades = filas.stream().map(this::aActividad).toList();
        return new MiDespliegueResponse(
                oferta.getId(),
                oferta.getEstado().name(),
                oferta.getEstado().getEtiqueta(),
                emergencia.getId(),
                emergencia.getZonaAfectada(),
                emergencia.getNivelGravedad().getEtiqueta(),
                emergencia.getMunicipio().getNombre(),
                oferta.getLote().getId(),
                oferta.getLote().getTitulo(),
                oferta.getOngs().stream().map(Ong::getRazonSocial).sorted().toList(),
                actividades.size(),
                (int) actividades.stream().filter(MiDespliegueResponse.Actividad::finalizada).count(),
                actividades);
    }

    private MiDespliegueResponse.Actividad aActividad(ActividadOferta avance) {
        return new MiDespliegueResponse.Actividad(
                avance.getActividad().getId(),
                avance.getActividad().getNombre(),
                avance.getActividad().getDescripcion(),
                avance.isFinalizada(),
                avance.getFechaFinalizacion(),
                avance.getOngFinalizacion() != null ? avance.getOngFinalizacion().getRazonSocial() : null,
                avance.getUsuarioFinalizacion());
    }

    private <T> T transaccion(Supplier<T> accion) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status -> accion.get());
    }
}
