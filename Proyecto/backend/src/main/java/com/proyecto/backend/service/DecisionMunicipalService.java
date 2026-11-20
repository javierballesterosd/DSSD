package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.*;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.exception.*;
import com.proyecto.backend.mapper.EmergenciaMapper;
import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionMunicipalService {
    private final EmergenciaRepository emergencias;
    private final LoteRepository lotes;
    private final OfertaRepository ofertas;
    private final InventarioOngRepository inventarios;
    private final BonitaClient bonita;
    private final EmergenciaMapper mapper;
    private final ApplicationEventPublisher eventos;
    private final PlatformTransactionManager transactionManager;

    @Transactional(readOnly = true)
    public RecursosMunicipalesResponse consultar(Long emergenciaId, Long municipioId) {
        Emergencia emergencia = buscar(emergenciaId, municipioId, false);
        Optional<Lote> ultimo = lotes.findFirstByEmergenciaIdOrderByIdDesc(emergenciaId);
        if (ultimo.isEmpty()) {
            return new RecursosMunicipalesResponse(null, null, false, false,
                    "Todavía no se publicó un lote para esta emergencia.", List.of());
        }
        Lote lote = ultimo.get();
        List<Oferta> conjunto = ofertas.findByLoteIdOrderByIdAsc(lote.getId());
        Map<Long, Recurso> catalogo = new TreeMap<>();
        Map<Long, Map<Long, Long>> cantidadPorRecursoYOng = new HashMap<>();
        Map<Long, Ong> ongsPorId = new HashMap<>();
        Map<Long, Long> requeridas = new HashMap<>();
        for (Oferta oferta : conjunto) {
            if (!habilitada(oferta) && !adjudicada(oferta)) continue;
            for (DetalleOferta detalle : oferta.getDetalles()) {
                int cantidad = cantidad(detalle);
                if (cantidad == 0) continue;
                Recurso recurso = detalle.getItemLote().getRecurso();
                catalogo.putIfAbsent(recurso.getId(), recurso);
                requeridas.putIfAbsent(recurso.getId(), (long) detalle.getItemLote().getCantidadRequerida());
                ongsPorId.putIfAbsent(detalle.getOng().getId(), detalle.getOng());
                cantidadPorRecursoYOng.computeIfAbsent(recurso.getId(), id -> new TreeMap<>())
                        .merge(detalle.getOng().getId(), (long) cantidad, Long::sum);
            }
        }
        List<RecursosMunicipalesResponse.RecursoAceptado> recursos = catalogo.values().stream().map(recurso -> {
            List<RecursosMunicipalesResponse.AporteOng> porOng = cantidadPorRecursoYOng.get(recurso.getId())
                    .entrySet().stream()
                    .map(e -> new RecursosMunicipalesResponse.AporteOng(e.getKey(),
                            ongsPorId.get(e.getKey()).getRazonSocial(), e.getValue()))
                    .toList();
            return new RecursosMunicipalesResponse.RecursoAceptado(recurso.getId(), recurso.getNombre(),
                    recurso.getUnidadMedida(), requeridas.get(recurso.getId()), porOng.stream().mapToLong(RecursosMunicipalesResponse.AporteOng::cantidad).sum(),
                    porOng);
        }).toList();
        boolean confirmada = lote.getFechaDecisionMunicipal() != null;
        String mensaje = confirmada ? (Boolean.TRUE.equals(lote.getDecisionMunicipal())
                ? "Aceptaste todos los recursos habilitados por el SN."
                : emergencia.isRequiereActualizacion() ? (lote.getDecisionMunicipal() == null
                        ? "El Sistema Nacional no habilitó recursos para ninguna oferta. Debés volver a registrar la "
                        + "emergencia, aunque mantengas los mismos datos, para reiniciar la convocatoria."
                        : "Debés actualizar la emergencia para reiniciar el proceso.")
                : "La emergencia fue actualizada; el coordinador puede publicar otro lote.")
                : lote.getDecisionMunicipal() != null ? "Estamos procesando la decisión en el Sistema Nacional."
                : impedimento(lote, conjunto);
        return new RecursosMunicipalesResponse(lote.getId(), lote.getDecisionMunicipal(), confirmada,
                !confirmada && (lote.getDecisionMunicipal() != null || mensaje == null), mensaje, recursos);
    }

    /** Se confirma la intención en una transacción independiente de los efectos externos.
     * SN admite reintentos idempotentes: ante un fallo se conserva la decisión y se retoma.
     */
    public void decidir(Long emergenciaId, DecisionMunicipalRequest request,
            LoginResponse usuario, BonitaSession session) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> {
            buscar(emergenciaId, usuario.getMunicipioId(), true);
            Lote lote = bloquearLote(request.loteId(), emergenciaId);
            if (lote.getDecisionMunicipal() != null) {
                if (!lote.getDecisionMunicipal().equals(request.aceptar())) {
                    throw new ReglaNegocioException("Ya hay una decisión registrada. Solo podés reintentar esa misma decisión.");
                }
                return;
            }
            List<Oferta> conjunto = ofertas.findByLoteIdOrderByIdAsc(lote.getId());
            String problema = impedimento(lote, conjunto);
            if (problema != null) throw new ReglaNegocioException(problema);
            if (request.aceptar() && conjunto.stream().noneMatch(o -> habilitada(o)
                    && o.getDetalles().stream().anyMatch(d -> cantidad(d) > 0))) {
                throw new ReglaNegocioException("El SN no habilitó recursos. Rechazá el conjunto para actualizar la emergencia.");
            }
            lote.setDecisionMunicipal(request.aceptar());
            lote.setUsuarioDecisionMunicipal(usuario.getUsername());
            lote.setUsuarioDecisionMunicipalId(usuario.getUserId());
            lotes.save(lote);
        });
        // La intención ya está confirmada antes de que Bonita pueda llamar al callback.
        String tarea = tx.execute(status -> {
            Emergencia emergencia = buscar(emergenciaId, usuario.getMunicipioId(), true);
            Lote lote = bloquearLote(request.loteId(), emergenciaId);
            if (lote.getFechaDecisionMunicipal() != null) return null;
            if (lote.getTareaDecisionBonitaId() == null) {
                lote.setTareaDecisionBonitaId(bonita.buscarTareaPendiente(session,
                        emergencia.getBonitaCaseId(), "Ver ofertas validadas"));
                lotes.save(lote);
            }
            return lote.getTareaDecisionBonitaId();
        });
        if (tarea != null) bonita.ejecutarTareaIdempotente(session, tarea,
                Map.of("aceptaOfertas", request.aceptar()));
    }

    /** Solo Bonita confirma efectos locales después de completar todas las operaciones nacionales. */
    @Transactional
    public void confirmar(Long emergenciaId, IntegracionNacional.Respuesta respuesta) {
        Emergencia emergencia = emergencias.bloquearPorId(emergenciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la emergencia"));
        Lote lote = bloquearLote(respuesta.loteId(), emergenciaId);
        if (!Objects.equals(emergencia.getBonitaCaseId(), respuesta.caseId())) {
            throw new ReglaNegocioException("El caso de Bonita no corresponde a la emergencia");
        }
        if (lote.getDecisionMunicipal() == null) throw new ReglaNegocioException("No hay decisión municipal registrada");
        if (lote.getFechaDecisionMunicipal() != null) return;
        boolean aceptar = lote.getDecisionMunicipal();
        List<Oferta> conjunto = ofertas.findByLoteIdOrderByIdAsc(lote.getId());
        List<Oferta> habilitadas = conjunto.stream().filter(this::habilitada).toList();
        Map<Long, IntegracionNacional.Resultado> resultados = new HashMap<>();
        for (var resultado : respuesta.resultados()) {
            if (!Boolean.TRUE.equals(resultado.aprobada())
                    || resultados.putIfAbsent(resultado.ofertaId(), resultado) != null) {
                throw new ReglaNegocioException("Resultados nacionales duplicados o sin confirmar");
            }
        }
        for (Oferta oferta : habilitadas) {
            var resultado = resultados.remove(oferta.getId());
            if (resultado == null) throw new ReglaNegocioException("Falta confirmar una oferta en SN");
            verificarReservas(oferta, resultado, aceptar);
        }
        if (!resultados.isEmpty()) throw new ReglaNegocioException("El resultado contiene ofertas ajenas al conjunto");
        if (aceptar) bloquearStock(habilitadas);
        LoginResponse usuario = new LoginResponse();
        usuario.setUserId(lote.getUsuarioDecisionMunicipalId());
        usuario.setUsername(lote.getUsuarioDecisionMunicipal());
        Set<String> destinatarios = new HashSet<>();
        for (Oferta oferta : conjunto) {
            if (oferta.getEstado() == EstadoOferta.ELIMINADA) continue;
            if (!aceptar || habilitada(oferta)) {
                oferta.getOngs().forEach(ong -> destinatarios.add(ong.getBonitaGroupPath()));
            }
            if (habilitada(oferta)) {
                oferta.setEstado(aceptar ? EstadoOferta.ADJUDICADA : EstadoOferta.NO_ADJUDICADA);
                oferta.setFechaModificacion(LocalDateTime.now());
                ofertas.save(oferta);
                if (aceptar) asignarActividades(emergencia, oferta);
            }
        }
        lote.setFechaDecisionMunicipal(LocalDateTime.now());
        if (!aceptar) {
            lote.setEstado(EstadoLote.CANCELADO);
            emergencia.setRequiereActualizacion(true);
            emergencias.save(emergencia);
        }
        lotes.saveAndFlush(lote);
        eventos.publishEvent(new DecisionMunicipalEvent(emergenciaId, emergencia.getZonaAfectada(), aceptar,
                emergencia.getMunicipio().getBonitaGroupPath(), Set.copyOf(destinatarios), usuario));
        log.info("Decisión municipal confirmada. emergenciaId={}, loteId={}, acepta={}, usuario={}",
                emergenciaId, lote.getId(), aceptar, usuario.getUsername());
    }

    /**
     * Cierre del lote cuando el SN no dejó recursos habilitados en ninguna oferta (no hay nada que decidir).
     * Se cancela el lote, la emergencia queda pendiente de nuevo registro y se avisa al municipio y a las ONGs.
     * Es idempotente: si el lote ya se cerró no hace nada, así Bonita puede reintentar.
     */
    @Transactional
    public void confirmarSinRecursos(Long emergenciaId, IntegracionNacional.SinRecursos pedido) {
        Emergencia emergencia = emergencias.bloquearPorId(emergenciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la emergencia"));
        Lote lote = bloquearLote(pedido.loteId(), emergenciaId);
        if (!Objects.equals(emergencia.getBonitaCaseId(), pedido.caseId())) {
            throw new ReglaNegocioException("El caso de Bonita no corresponde a la emergencia");
        }
        if (lote.getFechaDecisionMunicipal() != null) return;
        if (lote.getEstado() != EstadoLote.ACTIVO || lote.getDecisionMunicipal() != null
                || lote.getFechaCierreOfertas() == null || lote.getFechaCierreOfertas().isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("El lote ya no admite este cierre");
        }
        List<Oferta> conjunto = ofertas.findByLoteIdOrderByIdAsc(lote.getId());
        if (conjunto.stream().anyMatch(o -> o.getEstado() == EstadoOferta.PENDIENTE || habilitada(o))) {
            throw new ReglaNegocioException("El lote todavía tiene ofertas pendientes o habilitadas por el SN");
        }
        Set<String> destinatarios = new HashSet<>();
        conjunto.stream().filter(o -> o.getEstado() != EstadoOferta.ELIMINADA)
                .forEach(o -> o.getOngs().forEach(ong -> destinatarios.add(ong.getBonitaGroupPath())));
        lote.setEstado(EstadoLote.CANCELADO);
        lote.setFechaDecisionMunicipal(LocalDateTime.now());
        lotes.saveAndFlush(lote);
        emergencia.setRequiereActualizacion(true);
        emergencias.save(emergencia);
        eventos.publishEvent(new SinRecursosEvent(emergenciaId, emergencia.getZonaAfectada(),
                emergencia.getMunicipio().getBonitaGroupPath(),
                Set.copyOf(destinatarios)));
        log.info("Lote cancelado: el SN no habilitó recursos. emergenciaId={}, loteId={}", emergenciaId, lote.getId());
    }

    private void verificarReservas(Oferta oferta, IntegracionNacional.Resultado resultado, boolean aceptar) {
        Map<String, Long> esperadas = new HashMap<>();
        for (DetalleOferta detalle : oferta.getDetalles()) {
            if (cantidad(detalle) > 0) esperadas.merge(detalle.getOng().getCodigo() + ":" + detalle.getItemLote().getRecurso().getId(),
                    (long) cantidad(detalle), Math::addExact);
        }
        Map<String, Long> recibidas = new HashMap<>();
        for (var item : resultado.items()) {
            if (item.cantidad() <= 0 || !(aceptar ? "COMPROMETIDA".equals(item.estado())
                    : Set.of("LIBERADA", "EXPIRADA").contains(item.estado()))) {
                throw new ReglaNegocioException("El SN no confirmó el estado nacional esperado");
            }
            String clave = item.ongCodigo() + ":" + item.recursoId();
            if (!esperadas.containsKey(clave)) throw new ReglaNegocioException("El SN devolvió recursos ajenos a la oferta");
            recibidas.merge(clave, (long) item.cantidad(), Math::addExact);
        }
        // La liberación puede incluir historial de reservas reemplazadas; no puede dejar recursos activos.
        if (aceptar ? !esperadas.equals(recibidas) : esperadas.entrySet().stream()
                .anyMatch(e -> recibidas.getOrDefault(e.getKey(), 0L) < e.getValue())) {
            throw new ReglaNegocioException("Las reservas del SN no coinciden con las cantidades habilitadas");
        }
    }

    private void bloquearStock(List<Oferta> conjunto) {
        // Agrupar todo el conjunto y bloquear en orden estable, incluso entre distintos lotes.
        record Clave(Long ongId, Long recursoId) {}
        Map<Clave, Integer> cantidades = new TreeMap<>(Comparator.comparing(Clave::ongId)
                .thenComparing(Clave::recursoId));
        for (Oferta oferta : conjunto) {
            for (DetalleOferta detalle : oferta.getDetalles()) {
                if (cantidad(detalle) == 0) continue;
                cantidades.merge(new Clave(detalle.getOng().getId(), detalle.getItemLote().getRecurso().getId()),
                        cantidad(detalle), Math::addExact);
            }
        }
        Map<Clave, InventarioOng> bloqueados = new LinkedHashMap<>();
        cantidades.forEach((clave, cantidad) -> {
            InventarioOng inventario = inventarios.findByOngIdAndRecursoIdForUpdate(clave.ongId(), clave.recursoId())
                    .orElseThrow(() -> new ReglaNegocioException("No existe el inventario para bloquear los recursos."));
            if (inventario.getCantidadDisponible() < cantidad)
                throw new ReglaNegocioException("La ONG no tiene stock local suficiente para confirmar la adjudicación.");
            bloqueados.put(clave, inventario);
        });
        // Se valida todo antes de modificar. La confirmación y el descuento comparten transacción.
        bloqueados.forEach((clave, inventario) -> {
            inventario.setCantidadDisponible(Math.subtractExact(inventario.getCantidadDisponible(), cantidades.get(clave)));
            inventarios.save(inventario);
        });
    }

    @Transactional
    public EmergenciaResponse actualizar(Long emergenciaId, EmergenciaRequest request,
            LoginResponse usuario, BonitaSession session) {
        Emergencia emergencia = buscar(emergenciaId, usuario.getMunicipioId(), true);
        if (!emergencia.isRequiereActualizacion()) {
            throw new ReglaNegocioException("La emergencia no está pendiente de actualización por rechazo.");
        }
        emergencia.setDescripcion(request.descripcion());
        emergencia.setNivelGravedad(request.nivelGravedad());
        emergencia.setZonaAfectada(request.zonaAfectada());
        sincronizarActividades(emergencia, request.actividades());
        emergencia.setFechaActualizacion(LocalDateTime.now());
        emergencia.setRequiereActualizacion(false);
        emergencias.saveAndFlush(emergencia);
        Map<String, Object> contrato = new LinkedHashMap<>();
        contrato.put("emergenciaId", emergencia.getId());
        contrato.put("municipioId", emergencia.getMunicipio().getId());
        contrato.put("nivelGravedad", emergencia.getNivelGravedad().name());
        contrato.put("zonaAfectada", emergencia.getZonaAfectada());
        contrato.put("descripcion", emergencia.getDescripcion());
        contrato.put("regionGroupPath", emergencia.getMunicipio().getRegion().getBonitaGroupPath());
        String tarea = bonita.buscarTareaPendiente(session, emergencia.getBonitaCaseId(), "Registrar emergencia");
        bonita.ejecutarTarea(session, tarea, contrato);
        eventos.publishEvent(new EmergenciaRegistradaEvent(emergencia.getId(), emergencia.getMunicipio().getNombre(),
                emergencia.getNivelGravedad().name(), emergencia.getZonaAfectada(),
                emergencia.getMunicipio().getRegion().getBonitaGroupPath(), usuario));
        log.info("Emergencia actualizada tras rechazo. emergenciaId={}, usuario={}", emergenciaId, usuario.getUsername());
        return mapper.toResponse(emergencia);
    }

    /**
     * Deja las actividades de la emergencia como las envió el municipio: las existentes se editan en su lugar
     * (conservan su estado), las nuevas se agregan y las que sobran se quitan. Sin lista no se toca nada.
     */
    private void sincronizarActividades(Emergencia emergencia, List<ActividadRequest> pedidas) {
        if (pedidas == null) return;
        List<Actividad> actuales = emergencia.getActividades();
        for (int i = 0; i < pedidas.size(); i++) {
            ActividadRequest pedida = pedidas.get(i);
            if (i < actuales.size()) {
                actuales.get(i).setNombre(pedida.nombre());
                actuales.get(i).setDescripcion(pedida.descripcion());
            } else {
                Actividad nueva = new Actividad();
                nueva.setNombre(pedida.nombre());
                nueva.setDescripcion(pedida.descripcion());
                nueva.setEmergencia(emergencia);
                actuales.add(nueva);
            }
        }
        while (actuales.size() > pedidas.size()) actuales.remove(actuales.size() - 1);
    }

    private Emergencia buscar(Long id, Long municipioId, boolean bloquear) {
        Emergencia emergencia = (bloquear ? emergencias.bloquearPorId(id) : emergencias.findById(id))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la emergencia " + id));
        if (municipioId == null || !emergencia.getMunicipio().getId().equals(municipioId)) {
            throw new AccesoDenegadoException("La emergencia no pertenece a tu municipio.");
        }
        return emergencia;
    }

    private Lote bloquearLote(Long loteId, Long emergenciaId) {
        Lote lote = lotes.bloquearPorId(loteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el lote " + loteId));
        if (!lote.getEmergencia().getId().equals(emergenciaId)) {
            throw new ReglaNegocioException("El lote no pertenece a esta emergencia.");
        }
        return lote;
    }

    private String impedimento(Lote lote, List<Oferta> conjunto) {
        if (lote.getEstado() != EstadoLote.ACTIVO) return "El lote no está activo.";
        if (lote.getFechaCierreOfertas() == null || lote.getFechaCierreOfertas().isAfter(LocalDateTime.now())) {
            return "La convocatoria debe cerrar antes de decidir.";
        }
        if (conjunto.stream().anyMatch(o -> o.getEstado() == EstadoOferta.PENDIENTE)) {
            return "El Sistema Nacional todavía no terminó de validar las ofertas.";
        }
        if (conjunto.stream().noneMatch(o -> habilitada(o) || o.getEstado() == EstadoOferta.RECHAZADA)) {
            return "Todavía no hay recursos habilitados por el Sistema Nacional.";
        }
        return null;
    }

    private boolean habilitada(Oferta oferta) {
        return oferta.getEstado() == EstadoOferta.VALIDADA || oferta.getEstado() == EstadoOferta.ACEPTADA_PARCIAL;
    }

    /** Adjudicada, en despliegue o ya terminada: sigue siendo parte de los recursos aceptados. */
    private boolean adjudicada(Oferta oferta) {
        return oferta.getEstado() == EstadoOferta.ADJUDICADA || oferta.getEstado() == EstadoOferta.EN_EJECUCION
                || oferta.getEstado() == EstadoOferta.FINALIZADA;
    }

    /**
     * Cada oferta adjudicada con recursos debe completar todas las actividades de la emergencia: se crea una fila
     * de avance por actividad (idempotente, por si la confirmación se reintenta).
     */
    private void asignarActividades(Emergencia emergencia, Oferta oferta) {
        if (oferta.getDetalles().stream().noneMatch(d -> cantidad(d) > 0)) return;
        for (Actividad actividad : emergencia.getActividades()) {
            boolean existe = actividad.getAsignaciones().stream().anyMatch(a -> a.getOferta() == oferta
                    || (oferta.getId() != null && oferta.getId().equals(a.getOferta().getId())));
            if (existe) continue;
            ActividadOferta avance = new ActividadOferta();
            avance.setActividad(actividad);
            avance.setOferta(oferta);
            actividad.getAsignaciones().add(avance);
        }
    }

    private int cantidad(DetalleOferta detalle) {
        return detalle.getCantidadAceptada() == null ? 0 : detalle.getCantidadAceptada();
    }
}
