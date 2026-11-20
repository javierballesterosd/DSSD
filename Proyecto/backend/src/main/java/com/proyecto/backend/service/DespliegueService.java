package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.IntegracionNacional;
import com.proyecto.backend.exception.*;
import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/** La aplicación avisa a Bonita; exclusivamente el conector habla con el SN. */
@Service
@RequiredArgsConstructor
public class DespliegueService {
    private final EmergenciaRepository emergencias;
    private final ActividadOfertaRepository asignaciones;
    private final LoteRepository lotes;
    private final OfertaRepository ofertas;
    private final InventarioOngRepository inventarios;
    private final BonitaClient bonita;
    private final PlatformTransactionManager transactionManager;

    /** También permite reintentar el aviso sin modificar actividades ni depender de su cantidad. */
    public void completarMonitoreo(Long emergenciaId, Long regionId, BonitaSession sesion) {
        completarMonitoreo(emergenciaId, id -> autorizada(id, regionId), sesion);
    }

    /**
     * Cierra el monitoreo cuando todas las ofertas adjudicadas terminaron todas las actividades. Lo dispara quien
     * marca la última (la ONG), por eso no exige ser coordinador de la región.
     */
    public void verificarCierre(Long emergenciaId, BonitaSession sesion) {
        completarMonitoreo(emergenciaId, this::bloqueada, sesion);
    }

    private void completarMonitoreo(Long emergenciaId, Function<Long, Emergencia> acceso, BonitaSession sesion) {
        String caso = transaccion(() -> {
            Emergencia e = acceso.apply(emergenciaId);
            exigirAdjudicacion(e);
            if (e.getFechaFinMonitoreo() != null || hayPendientes(emergenciaId))
                return null;
            return e.getBonitaCaseId();
        });
        if (caso == null) return;
        // No mantener locks mientras se espera a Bonita.
        String guardada = transaccion(() -> acceso.apply(emergenciaId).getTareaMonitoreoBonitaId());
        String encontrada = guardada != null ? guardada : bonita.buscarTareaPendiente(sesion, caso, "Monitorear despliegue");
        String tarea = transaccion(() -> {
            Emergencia e = acceso.apply(emergenciaId);
            if (e.getFechaFinMonitoreo() != null) return null;
            if (hayPendientes(emergenciaId))
                throw new ReglaNegocioException("Todavía quedan actividades pendientes");
            if (e.getTareaMonitoreoBonitaId() == null) e.setTareaMonitoreoBonitaId(encontrada);
            emergencias.save(e);
            return e.getTareaMonitoreoBonitaId();
        });
        if (tarea == null) return;
        bonita.ejecutarTareaIdempotente(sesion, tarea, Map.of("hayActividadesPendientes", false));
        transaccion(() -> {
            Emergencia e = acceso.apply(emergenciaId);
            if (e.getFechaFinMonitoreo() == null) e.setFechaFinMonitoreo(LocalDateTime.now());
            emergencias.save(e);
            return null;
        });
    }

    public void validarDespliegue(Long emergenciaId, Long regionId, BonitaSession sesion) {
        completarMonitoreo(emergenciaId, regionId, sesion);
        String caso = transaccion(() -> {
            Emergencia e = autorizada(emergenciaId, regionId);
            exigirSinPendientes(e);
            return e.getFechaFinalizacion() == null ? e.getBonitaCaseId() : null;
        });
        if (caso == null) return;
        String guardada = transaccion(() -> autorizada(emergenciaId, regionId).getTareaValidacionDespliegueBonitaId());
        String encontrada = guardada != null ? guardada : bonita.buscarTareaPendiente(sesion, caso, "Validar estado despliegue");
        String tarea = transaccion(() -> {
            Emergencia e = autorizada(emergenciaId, regionId);
            exigirSinPendientes(e);
            if (e.getFechaFinalizacion() != null) return null;
            if (e.getTareaValidacionDespliegueBonitaId() == null) e.setTareaValidacionDespliegueBonitaId(encontrada);
            // El conector solo puede liberar después de esta aprobación persistida.
            if (e.getFechaValidacionDespliegue() == null) e.setFechaValidacionDespliegue(LocalDateTime.now());
            emergencias.save(e);
            return e.getTareaValidacionDespliegueBonitaId();
        });
        if (tarea != null) bonita.ejecutarTareaIdempotente(sesion, tarea, Map.of());
    }

    @Transactional(readOnly = true)
    public IntegracionNacional.Plan plan(Long id, Long loteId, String caseId) {
        Lote lote = comprobar(id, loteId, caseId, false);
        Emergencia e = lote.getEmergencia();
        exigirSinPendientes(e);
        if (e.getFechaValidacionDespliegue() == null)
            throw new ReglaNegocioException("El coordinador todavía no validó el despliegue");
        var conjunto = ofertas.findByLoteIdOrderByIdAsc(loteId).stream().filter(this::asignada)
                .map(o -> new IntegracionNacional.Oferta(o.getId(), o.getDetalles().stream()
                        .filter(d -> cantidad(d) > 0)
                        .map(d -> new IntegracionNacional.Item(d.getId(), d.getOng().getCodigo(),
                                d.getItemLote().getRecurso().getId(), cantidad(d))).toList())).toList();
        if (e.getFechaFinalizacion() == null && conjunto.isEmpty())
            throw new ReglaNegocioException("No hay ofertas adjudicadas para finalizar");
        return new IntegracionNacional.Plan(id, loteId, caseId, true, e.getFechaFinalizacion() != null, true, conjunto);
    }

    @Transactional
    public void confirmar(Long id, IntegracionNacional.Respuesta respuesta) {
        emergencias.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la emergencia"));
        Lote lote = comprobar(id, respuesta.loteId(), respuesta.caseId(), true);
        Emergencia e = lote.getEmergencia();
        if (e.getFechaFinalizacion() != null) return;
        exigirSinPendientes(e);
        if (e.getFechaValidacionDespliegue() == null)
            throw new ReglaNegocioException("Falta la validación del coordinador");
        var conjunto = ofertas.findByLoteIdOrderByIdAsc(lote.getId()).stream().filter(this::asignada).toList();
        if (conjunto.isEmpty()) throw new ReglaNegocioException("No hay ofertas adjudicadas para finalizar");
        Map<Long, IntegracionNacional.Resultado> recibos = new HashMap<>();
        for (var r : respuesta.resultados()) {
            if (!Boolean.TRUE.equals(r.aprobada()) || recibos.putIfAbsent(r.ofertaId(), r) != null)
                throw new ReglaNegocioException("Resultado nacional duplicado o sin confirmar");
        }
        // Primero comprobar todo el conjunto; luego devolver stock exactamente una vez.
        for (Oferta o : conjunto) verificar(o, recibos.remove(o.getId()));
        if (!recibos.isEmpty()) throw new ReglaNegocioException("El resultado contiene ofertas ajenas al despliegue");
        var detalles = conjunto.stream().flatMap(o -> o.getDetalles().stream()).filter(d -> cantidad(d) > 0)
                .sorted(Comparator.comparing((DetalleOferta d) -> d.getOng().getId())
                        .thenComparing(d -> d.getItemLote().getRecurso().getId())).toList();
        for (DetalleOferta d : detalles) {
            InventarioOng inventario = inventarios.findByOngIdAndRecursoIdForUpdate(
                    d.getOng().getId(), d.getItemLote().getRecurso().getId())
                    .orElseThrow(() -> new ReglaNegocioException("No existe el inventario a liberar"));
            inventario.setCantidadDisponible(Math.addExact(inventario.getCantidadDisponible(), cantidad(d)));
            inventarios.save(inventario);
        }
        for (Oferta o : conjunto) {
            o.setEstado(EstadoOferta.FINALIZADA);
            o.setFechaModificacion(LocalDateTime.now());
            ofertas.save(o);
        }
        lote.setEstado(EstadoLote.FINALIZADO);
        lotes.save(lote);
        e.setFechaFinalizacion(LocalDateTime.now());
        emergencias.save(e);
    }

    private void verificar(Oferta oferta, IntegracionNacional.Resultado recibo) {
        if (recibo == null) throw new ReglaNegocioException("Falta confirmar una liberación nacional");
        Map<String, Long> esperadas = new HashMap<>(), recibidas = new HashMap<>();
        for (DetalleOferta d : oferta.getDetalles()) if (cantidad(d) > 0)
            esperadas.merge(d.getOng().getCodigo() + ":" + d.getItemLote().getRecurso().getId(), (long) cantidad(d), Math::addExact);
        for (var i : recibo.items()) {
            String clave = i.ongCodigo() + ":" + i.recursoId();
            if (!Set.of("LIBERADA", "EXPIRADA").contains(i.estado()) || i.cantidad() <= 0 || !esperadas.containsKey(clave))
                throw new ReglaNegocioException("El SN no confirmó la liberación de los recursos esperados");
            // El historial puede contener bloqueos vencidos; solo cuenta lo efectivamente liberado.
            if ("LIBERADA".equals(i.estado())) recibidas.merge(clave, (long) i.cantidad(), Math::addExact);
        }
        if (esperadas.entrySet().stream().anyMatch(i -> recibidas.getOrDefault(i.getKey(), 0L) < i.getValue()))
            throw new ReglaNegocioException("Faltan cantidades liberadas en SN");
    }

    private Emergencia bloqueada(Long id) {
        return emergencias.bloquearPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la emergencia"));
    }

    private Emergencia autorizada(Long id, Long regionId) {
        Emergencia e = bloqueada(id);
        if (regionId == null || !regionId.equals(e.getMunicipio().getRegion().getId()))
            throw new AccesoDenegadoException("La emergencia no pertenece a tu región");
        return e;
    }

    private void exigirAdjudicacion(Emergencia e) {
        Lote lote = lotes.findFirstByEmergenciaIdOrderByIdDesc(e.getId())
                .orElseThrow(() -> new ReglaNegocioException("No existe un lote adjudicado"));
        if (!Boolean.TRUE.equals(lote.getDecisionMunicipal()) || lote.getFechaDecisionMunicipal() == null)
            throw new ReglaNegocioException("El municipio todavía no confirmó la adjudicación");
    }

    private void exigirSinPendientes(Emergencia e) {
        if (e.getFechaFinMonitoreo() == null || hayPendientes(e.getId()))
            throw new ReglaNegocioException("El monitoreo todavía tiene actividades pendientes");
    }

    /** Pendiente si alguna oferta adjudicada no terminó una actividad, o si todavía no hay actividades asignadas. */
    private boolean hayPendientes(Long emergenciaId) {
        return !asignaciones.existsByActividadEmergenciaId(emergenciaId)
                || asignaciones.existsByActividadEmergenciaIdAndFinalizadaFalse(emergenciaId);
    }

    private Lote comprobar(Long id, Long loteId, String caseId, boolean bloquear) {
        Lote lote = (bloquear ? lotes.bloquearPorId(loteId) : lotes.findById(loteId))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el lote"));
        if (!lote.getEmergencia().getId().equals(id) || !Objects.equals(lote.getEmergencia().getBonitaCaseId(), caseId)
                || !Boolean.TRUE.equals(lote.getDecisionMunicipal()) || lote.getFechaDecisionMunicipal() == null)
            throw new ReglaNegocioException("El lote o el caso no corresponde al despliegue adjudicado");
        return lote;
    }

    private boolean asignada(Oferta o) {
        return o.getEstado() == EstadoOferta.ADJUDICADA || o.getEstado() == EstadoOferta.EN_EJECUCION;
    }
    private int cantidad(DetalleOferta d) { return d.getCantidadAceptada() == null ? 0 : d.getCantidadAceptada(); }
    private <T> T transaccion(Supplier<T> accion) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status -> accion.get());
    }
}
