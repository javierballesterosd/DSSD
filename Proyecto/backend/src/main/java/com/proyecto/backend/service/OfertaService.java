package com.proyecto.backend.service;

import com.proyecto.backend.dto.DetalleOfertaRequest;
import com.proyecto.backend.dto.ItemValidadoRequest;
import com.proyecto.backend.dto.OfertaEdicionRequest;
import com.proyecto.backend.dto.OfertaRequest;
import com.proyecto.backend.dto.OfertaResponse;
import com.proyecto.backend.dto.OfertaVersionResponse;
import com.proyecto.backend.dto.ValidacionSNRequest;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.mapper.OfertaMapper;
import com.proyecto.backend.model.DetalleOferta;
import com.proyecto.backend.model.DetalleOfertaVersion;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.EstadoOferta;
import com.proyecto.backend.model.InventarioOng;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.model.Oferta;
import com.proyecto.backend.model.OfertaVersion;
import com.proyecto.backend.model.Ong;
import com.proyecto.backend.model.TipoCambioOferta;
import com.proyecto.backend.repository.InventarioOngRepository;
import com.proyecto.backend.repository.ItemLoteRepository;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.OfertaRepository;
import com.proyecto.backend.repository.OngRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfertaService {

    private final LoteRepository loteRepository;
    private final OngRepository ongRepository;
    private final ItemLoteRepository itemLoteRepository;
    private final InventarioOngRepository inventarioOngRepository;
    private final OfertaRepository ofertaRepository;
    private final OfertaMapper ofertaMapper;


    @Transactional
    public OfertaResponse registrar(OfertaRequest request, Long ongIdUsuario, String username) {
        Lote lote = loteRepository.findById(request.loteId())
                .orElseThrow(() -> rechazar("El lote no existe"));
        validarConvocatoriaAbierta(lote);

        List<Ong> ongs = ongRepository.findAllById(request.ongIds());
        if (ongs.size() != request.ongIds().size()) {
            throw rechazar("Alguna de las ONGs seleccionadas no existe");
        }
        Map<Long, Ong> ongsPorId = new HashMap<>();
        ongs.forEach(ong -> ongsPorId.put(ong.getId(), ong));

        // La ONG del usuario logueado participa siempre de la oferta
        if (!ongsPorId.containsKey(ongIdUsuario)) {
            String nombre = ongRepository.findById(ongIdUsuario)
                    .map(Ong::getRazonSocial)
                    .orElse(String.valueOf(ongIdUsuario));
            throw rechazar("La oferta debe incluir a tu ONG, «" + nombre + "»");
        }

        Map<Long, ItemLote> itemsDelLote = validarDetalles(lote, ongsPorId, request.detalles());

        Oferta oferta = new Oferta();
        oferta.setEstado(EstadoOferta.PENDIENTE);
        oferta.setFechaOferta(LocalDateTime.now());
        oferta.setLote(lote);
        oferta.setOngs(new HashSet<>(ongs));

        for (DetalleOfertaRequest detalleRequest : request.detalles()) {
            oferta.getDetalles().add(nuevoDetalle(oferta, itemsDelLote, ongsPorId, detalleRequest));
        }
        registrarVersion(oferta, TipoCambioOferta.CREACION, username, ongsPorId.get(ongIdUsuario));

        Oferta guardada = ofertaRepository.save(oferta);
        log.info("Oferta registrada. ofertaId={}, loteId={}, ongId={}, usuario={}, ongs={}",
                guardada.getId(), lote.getId(), ongIdUsuario, username, ongs.size());
        return ofertaMapper.toResponse(guardada);
    }

    /**
     * Cambia las cantidades de una oferta dentro de la ventana del lote. Las ONGs participantes
     * no cambian: para eso se elimina la oferta y se registra otra.
     */
    @Transactional
    public OfertaResponse actualizar(Long ofertaId, OfertaEdicionRequest request, Long ongIdUsuario,
                                     String username) {
        Oferta oferta = buscarModificable(ofertaId, ongIdUsuario);

        Map<Long, Ong> ongsPorId = new HashMap<>();
        oferta.getOngs().forEach(ong -> ongsPorId.put(ong.getId(), ong));
        Map<Long, ItemLote> itemsDelLote = validarDetalles(oferta.getLote(), ongsPorId, request.detalles());

        // Se actualiza en el lugar en vez de borrar y recrear: Hibernate inserta antes de borrar
        // al hacer flush y eso violaría el unique (oferta_id, item_lote_id, ong_id).
        Map<String, DetalleOferta> existentes = new HashMap<>();
        oferta.getDetalles().forEach(detalle -> existentes.put(
                clave(detalle.getItemLote().getId(), detalle.getOng().getId()), detalle));

        Set<String> clavesPedidas = new HashSet<>();
        for (DetalleOfertaRequest detalleRequest : request.detalles()) {
            String clave = clave(detalleRequest.itemLoteId(), detalleRequest.ongId());
            clavesPedidas.add(clave);
            DetalleOferta existente = existentes.get(clave);
            if (existente != null) {
                existente.setCantidadOfrecida(detalleRequest.cantidadOfrecida());
            } else {
                oferta.getDetalles().add(nuevoDetalle(oferta, itemsDelLote, ongsPorId, detalleRequest));
            }
        }
        oferta.getDetalles().removeIf(detalle -> !clavesPedidas.contains(
                clave(detalle.getItemLote().getId(), detalle.getOng().getId())));

        oferta.setFechaModificacion(LocalDateTime.now());
        registrarVersion(oferta, TipoCambioOferta.EDICION, username, ongsPorId.get(ongIdUsuario));
        Oferta guardada = ofertaRepository.save(oferta);
        log.info("Oferta {} editada por la ONG {}", ofertaId, ongIdUsuario);
        return ofertaMapper.toResponse(guardada);
    }

    /** Baja lógica: la oferta pasa a ELIMINADA y conserva sus detalles para trazabilidad. */
    @Transactional
    public void eliminar(Long ofertaId, Long ongIdUsuario, String username) {
        Oferta oferta = buscarModificable(ofertaId, ongIdUsuario);
        oferta.setEstado(EstadoOferta.ELIMINADA);
        oferta.setFechaModificacion(LocalDateTime.now());
        Ong ongAutor = oferta.getOngs().stream()
                .filter(ong -> ong.getId().equals(ongIdUsuario)).findFirst().orElseThrow();
        registrarVersion(oferta, TipoCambioOferta.BAJA, username, ongAutor);
        ofertaRepository.save(oferta);
        log.info("Oferta {} eliminada por la ONG {}", ofertaId, ongIdUsuario);
    }

    /** Ids de los lotes en los que la ONG tiene alguna oferta vigente (para marcar "ya ofertó" en listados). */
    @Transactional(readOnly = true)
    public List<Long> lotesConOfertaDeOng(Long ongId) {
        return ofertaRepository.findLoteIdsConOfertaDeOng(ongId);
    }

    /** Ofertas del lote en las que participa la ONG indicada, de la más nueva a la más vieja. */
    @Transactional(readOnly = true)
    public List<OfertaResponse> listarDeOng(Long loteId, Long ongId) {
        return ofertaRepository.findByLoteIdAndOngId(loteId, ongId).stream()
                .map(ofertaMapper::toResponse)
                .toList();
    }

    /** Historial de versiones de una oferta, de la más nueva a la más vieja. Lo ven las ONGs participantes y el auditor. */
    @Transactional(readOnly = true)
    public List<OfertaVersionResponse> listarVersiones(Long ofertaId, String rol, Long ongIdUsuario) {
        Oferta oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La oferta " + ofertaId + " no existe"));
        if ("ONG".equals(rol)) {
            exigirParticipacion(oferta, ongIdUsuario);
        } else if (!"AUDITOR".equals(rol)) {
            throw accesoDenegado("No tenés permiso para ver el historial de ofertas");
        }
        return oferta.getVersiones().stream()
                .sorted(Comparator.comparing(OfertaVersion::getNumero).reversed())
                .map(ofertaMapper::toVersionResponse)
                .toList();
    }

    /** Todas las ofertas, incluidas las eliminadas, para auditoría. */
    @Transactional(readOnly = true)
    public List<OfertaResponse> listarTodas(Long loteId) {
        return ofertaRepository.findAllParaAuditoria(loteId).stream()
                .map(ofertaMapper::toResponse)
                .toList();
    }

    /** Guarda una foto de la oferta tal como quedó tras el cambio. */
    private void registrarVersion(Oferta oferta, TipoCambioOferta tipo, String username, Ong ongAutor) {
        oferta.setNumeroVersion(oferta.getNumeroVersion() + 1);

        OfertaVersion version = new OfertaVersion();
        version.setOferta(oferta);
        version.setNumero(oferta.getNumeroVersion());
        version.setTipoCambio(tipo);
        version.setEstado(oferta.getEstado());
        version.setFecha(LocalDateTime.now());
        version.setUsuario(username);
        version.setOng(ongAutor);
        for (DetalleOferta detalle : oferta.getDetalles()) {
            DetalleOfertaVersion copia = new DetalleOfertaVersion();
            copia.setVersion(version);
            copia.setItemLote(detalle.getItemLote());
            copia.setOng(detalle.getOng());
            copia.setCantidadOfrecida(detalle.getCantidadOfrecida());
            version.getDetalles().add(copia);
        }
        oferta.getVersiones().add(version);
    }

    /** Oferta que la ONG del usuario puede editar o eliminar: participa, está pendiente y la ventana sigue abierta. */
    private Oferta buscarModificable(Long ofertaId, Long ongIdUsuario) {
        Oferta oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La oferta " + ofertaId + " no existe"));
        exigirParticipacion(oferta, ongIdUsuario);
        if (oferta.getEstado() != EstadoOferta.PENDIENTE) {
            throw rechazar("La oferta está en estado «" + oferta.getEstado().getEtiqueta()
                    + "» y ya no se puede modificar");
        }
        validarConvocatoriaAbierta(oferta.getLote());
        return oferta;
    }

    /** Rechazo de negocio: se registra en el log (warn) y se devuelve la excepción para lanzarla. */
    private ReglaNegocioException rechazar(String mensaje) {
        log.warn("Operación sobre ofertas rechazada: {}", mensaje);
        return new ReglaNegocioException(mensaje);
    }

    private AccesoDenegadoException accesoDenegado(String mensaje) {
        log.warn("Acceso denegado sobre ofertas: {}", mensaje);
        return new AccesoDenegadoException(mensaje);
    }

    private void exigirParticipacion(Oferta oferta, Long ongIdUsuario) {
        boolean participa = oferta.getOngs().stream().anyMatch(ong -> ong.getId().equals(ongIdUsuario));
        if (!participa) {
            throw accesoDenegado("Tu ONG no participa de esta oferta");
        }
    }

    private void validarConvocatoriaAbierta(Lote lote) {
        if (lote.getEstado() != EstadoLote.ACTIVO) {
            throw rechazar("El lote no está abierto a ofertas");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime apertura = lote.getFechaAperturaOfertas();
        if (apertura != null && apertura.isAfter(ahora)) {
            throw rechazar("La convocatoria de ofertas para este lote todavía no abrió");
        }
        LocalDateTime cierre = lote.getFechaCierreOfertas();
        if (cierre != null && !cierre.isAfter(ahora)) {
            throw rechazar("La convocatoria de ofertas para este lote ya cerró");
        }
    }

    /**
     * Valida las celdas contra el lote, las ONGs de la oferta y su inventario, y que cada ONG
     * aporte algo. Devuelve los ítems del lote indexados por id.
     */
    private Map<Long, ItemLote> validarDetalles(Lote lote, Map<Long, Ong> ongsPorId,
                                                List<DetalleOfertaRequest> detalles) {
        Map<Long, ItemLote> itemsDelLote = new HashMap<>();
        lote.getItems().forEach(item -> itemsDelLote.put(item.getId(), item));
        for (DetalleOfertaRequest detalle : detalles) {
            if (!itemsDelLote.containsKey(detalle.itemLoteId())) {
                throw rechazar(
                        "El item " + detalle.itemLoteId() + " no pertenece al lote " + lote.getId());
            }
            if (!ongsPorId.containsKey(detalle.ongId())) {
                throw rechazar(
                        "La ONG " + detalle.ongId() + " no está entre las ONGs de la oferta");
            }
        }

        Set<String> paresVistos = new HashSet<>();
        for (DetalleOfertaRequest detalle : detalles) {
            if (!paresVistos.add(clave(detalle.itemLoteId(), detalle.ongId()))) {
                throw rechazar(
                        "Hay más de una celda cargada para el mismo item y la misma ONG");
            }
        }

        Map<String, Integer> inventarioPorOngYRecurso = indexarInventario(ongsPorId.keySet());
        for (DetalleOfertaRequest detalle : detalles) {
            ItemLote item = itemsDelLote.get(detalle.itemLoteId());
            Ong ong = ongsPorId.get(detalle.ongId());
            Integer disponible = inventarioPorOngYRecurso.get(clave(detalle.ongId(), item.getRecurso().getId()));
            if (disponible == null || detalle.cantidadOfrecida() > disponible) {
                int maximo = disponible == null ? 0 : disponible;
                throw rechazar(
                        "La ONG «" + ong.getRazonSocial() + "» tiene " + maximo + " "
                                + item.getRecurso().getUnidadMedida() + " disponibles de "
                                + item.getRecurso().getNombre());
            }
        }

        Set<Long> ongsQueAportan = new HashSet<>();
        detalles.forEach(detalle -> ongsQueAportan.add(detalle.ongId()));
        for (Ong ong : ongsPorId.values()) {
            if (!ongsQueAportan.contains(ong.getId())) {
                throw rechazar(
                        "La ONG «" + ong.getRazonSocial() + "» no ofrece ningún recurso");
            }
        }
        return itemsDelLote;
    }

    private DetalleOferta nuevoDetalle(Oferta oferta, Map<Long, ItemLote> itemsDelLote,
                                       Map<Long, Ong> ongsPorId, DetalleOfertaRequest request) {
        DetalleOferta detalle = new DetalleOferta();
        detalle.setOferta(oferta);
        detalle.setItemLote(itemsDelLote.get(request.itemLoteId()));
        detalle.setOng(ongsPorId.get(request.ongId()));
        detalle.setCantidadOfrecida(request.cantidadOfrecida());
        return detalle;
    }

    private static String clave(Long primero, Long segundo) {
        return primero + ":" + segundo;
    }

    private Map<String, Integer> indexarInventario(Set<Long> ongIds) {
        Map<String, Integer> indice = new HashMap<>();
        for (InventarioOng inventario : inventarioOngRepository.findByOngIdIn(ongIds)) {
            indice.put(clave(inventario.getOng().getId(), inventario.getRecurso().getId()),
                    inventario.getCantidadDisponible());
        }
        return indice;
    }

    @Transactional
    public void procesarValidacionSNYBloquearRecursos(Long ofertaId, ValidacionSNRequest validacion) {
        Long loteId = ofertaRepository.findLoteIdByOfertaId(ofertaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La oferta no existe"));
        Lote lote = loteRepository.bloquearPorId(loteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El lote no existe"));
        Oferta oferta = ofertaRepository.findById(ofertaId).orElseThrow();
        if (validacion.aprobada() == null) throw rechazar("Debe indicarse si el SN aprobó la oferta");

        Map<Long, Integer> cantidades = new HashMap<>();
        if (validacion.aprobada()) {
            if (validacion.itemsValidados() == null || validacion.itemsValidados().isEmpty()) {
                throw rechazar("La aprobación debe indicar las cantidades aceptadas");
            }
            for (ItemValidadoRequest item : validacion.itemsValidados()) {
                if (item == null) throw rechazar("La validación contiene un detalle vacío");
                DetalleOferta detalle = buscarDetalleValidado(oferta, item);
                if (item.cantidadAceptada() == null || item.cantidadAceptada() < 0
                        || item.cantidadAceptada() > detalle.getCantidadOfrecida()
                        || cantidades.putIfAbsent(detalle.getId(), item.cantidadAceptada()) != null) {
                    throw rechazar("La validación contiene cantidades inválidas o detalles repetidos");
                }
            }
        }
        // Repetir el mismo callback no vuelve a descontar inventario.
        if (oferta.getEstado() != EstadoOferta.PENDIENTE) {
            boolean mismoResultado = oferta.getEstado() != EstadoOferta.ELIMINADA
                    && oferta.getDetalles().stream().allMatch(d ->
                    (d.getCantidadAceptada() == null ? 0 : d.getCantidadAceptada())
                            == cantidades.getOrDefault(d.getId(), 0));
            if (mismoResultado) return;
            throw rechazar("La oferta ya fue procesada con otra validación");
        }
        if (lote.getEstado() != EstadoLote.ACTIVO || lote.getDecisionMunicipal() != null) {
            throw rechazar("El lote ya tiene una decisión municipal o está cancelado");
        }
        boolean parcial = false;
        boolean tieneRecursos = false;
        var detalles = oferta.getDetalles().stream().sorted(Comparator
                .comparing((DetalleOferta d) -> d.getOng().getId())
                .thenComparing(d -> d.getItemLote().getRecurso().getId())).toList();
        for (DetalleOferta detalle : detalles) {
            int aceptada = cantidades.getOrDefault(detalle.getId(), 0);
            detalle.setCantidadAceptada(aceptada);
            parcial |= aceptada < detalle.getCantidadOfrecida();
            if (aceptada == 0) continue;
            tieneRecursos = true;
            InventarioOng inventario = inventarioOngRepository.findByOngIdAndRecursoIdForUpdate(
                    detalle.getOng().getId(), detalle.getItemLote().getRecurso().getId())
                    .orElseThrow(() -> rechazar("No existe el inventario para bloquear los recursos"));
            if (inventario.getCantidadDisponible() < aceptada) {
                throw rechazar("La ONG ya no tiene stock suficiente localmente");
            }
            inventario.setCantidadDisponible(inventario.getCantidadDisponible() - aceptada);
            inventarioOngRepository.save(inventario);
        }
        oferta.setEstado(!tieneRecursos ? EstadoOferta.RECHAZADA
                : parcial ? EstadoOferta.ACEPTADA_PARCIAL : EstadoOferta.VALIDADA);
        ofertaRepository.save(oferta);
        log.info("Validación SN registrada. ofertaId={}, estado={}", ofertaId, oferta.getEstado());
    }

    @Transactional
    public void liberarRecursosOferta(Long ofertaId, EstadoOferta nuevoEstado) {
        Oferta oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

        // 1. Validar que el cambio de estado tenga sentido para una liberación
        if (nuevoEstado != EstadoOferta.NO_ADJUDICADA && nuevoEstado != EstadoOferta.FINALIZADA) {
            throw new IllegalArgumentException("Estado inválido. Solo se liberan recursos al pasar a NO_ADJUDICADA o FINALIZADA");
        }

        // 2. Validar que no estemos liberando una oferta que ya fue liberada o rechazada
        EstadoOferta estadoActual = oferta.getEstado();
        if (estadoActual == EstadoOferta.NO_ADJUDICADA || estadoActual == EstadoOferta.FINALIZADA || estadoActual == EstadoOferta.RECHAZADA) {
            throw new IllegalStateException("La oferta ya se encuentra en un estado donde sus recursos no están comprometidos");
        }

        // 3. Devolver el stock ítem por ítem
        for (DetalleOferta detalle : oferta.getDetalles()) {
            
            // Solo devolvemos si el SN había aceptado una cantidad mayor a 0
            if (detalle.getCantidadAceptada() != null && detalle.getCantidadAceptada() > 0) {
                
                // Bloqueo Local: Aseguramos la fila para sumar con seguridad
                InventarioOng inventario = inventarioOngRepository.findByOngIdAndRecursoIdForUpdate(
                        detalle.getOng().getId(), 
                        detalle.getItemLote().getRecurso().getId()
                ).orElseThrow(() -> new RuntimeException("Inventario no encontrado al intentar liberar recursos"));

                // Sumamos la cantidad que teníamos retenida
                inventario.setCantidadDisponible(inventario.getCantidadDisponible() + detalle.getCantidadAceptada());
                inventarioOngRepository.save(inventario);
            }
        }

        // 4. Actualizamos el estado final
        oferta.setEstado(nuevoEstado);
        ofertaRepository.save(oferta);
    }

    private DetalleOferta buscarDetalleValidado(Oferta oferta, ItemValidadoRequest item) {
        return oferta.getDetalles().stream().filter(d -> item.detalleOfertaId() != null
                ? d.getId().equals(item.detalleOfertaId())
                : d.getItemLote().getId().equals(item.itemLoteId()) && d.getOng().getId().equals(item.ongId()))
                .findFirst().orElseThrow(() -> rechazar("El detalle no pertenece a la oferta"));
    }
}
