package com.proyecto.backend.service;

import com.proyecto.backend.dto.DetalleOfertaRequest;
import com.proyecto.backend.dto.OfertaRequest;
import com.proyecto.backend.dto.OfertaResponse;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.mapper.OfertaMapper;
import com.proyecto.backend.model.DetalleOferta;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.EstadoOferta;
import com.proyecto.backend.model.InventarioOng;
import com.proyecto.backend.model.ItemLote;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.model.Oferta;
import com.proyecto.backend.model.Ong;
import com.proyecto.backend.repository.InventarioOngRepository;
import com.proyecto.backend.repository.ItemLoteRepository;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.OfertaRepository;
import com.proyecto.backend.repository.OngRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OfertaService {

    private final LoteRepository loteRepository;
    private final OngRepository ongRepository;
    private final ItemLoteRepository itemLoteRepository;
    private final InventarioOngRepository inventarioOngRepository;
    private final OfertaRepository ofertaRepository;
    private final OfertaMapper ofertaMapper;

    public OfertaService(LoteRepository loteRepository, OngRepository ongRepository,
                          ItemLoteRepository itemLoteRepository, InventarioOngRepository inventarioOngRepository,
                          OfertaRepository ofertaRepository, OfertaMapper ofertaMapper) {
        this.loteRepository = loteRepository;
        this.ongRepository = ongRepository;
        this.itemLoteRepository = itemLoteRepository;
        this.inventarioOngRepository = inventarioOngRepository;
        this.ofertaRepository = ofertaRepository;
        this.ofertaMapper = ofertaMapper;
    }

    @Transactional
    public OfertaResponse registrar(OfertaRequest request, Long ongIdUsuario) {
        Lote lote = loteRepository.findById(request.loteId())
                .orElseThrow(() -> new ReglaNegocioException("El lote no existe"));

        if (lote.getEstado() != EstadoLote.ACTIVO) {
            throw new ReglaNegocioException("El lote no está abierto a ofertas");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime apertura = lote.getFechaAperturaOfertas();
        if (apertura != null && apertura.isAfter(ahora)) {
            throw new ReglaNegocioException("La convocatoria de ofertas para este lote todavía no abrió");
        }
        LocalDateTime cierre = lote.getFechaCierreOfertas();
        if (cierre != null && !cierre.isAfter(ahora)) {
            throw new ReglaNegocioException("La convocatoria de ofertas para este lote ya cerró");
        }

        List<Ong> ongs = ongRepository.findAllById(request.ongIds());
        if (ongs.size() != request.ongIds().size()) {
            throw new ReglaNegocioException("Alguna de las ONGs seleccionadas no existe");
        }
        Map<Long, Ong> ongsPorId = new HashMap<>();
        ongs.forEach(ong -> ongsPorId.put(ong.getId(), ong));

        // La ONG del usuario logueado participa siempre de la oferta
        if (!ongsPorId.containsKey(ongIdUsuario)) {
            String nombre = ongRepository.findById(ongIdUsuario)
                    .map(Ong::getRazonSocial)
                    .orElse(String.valueOf(ongIdUsuario));
            throw new ReglaNegocioException("La oferta debe incluir a tu ONG, «" + nombre + "»");
        }

        Map<Long, ItemLote> itemsDelLote = new HashMap<>();
        lote.getItems().forEach(item -> itemsDelLote.put(item.getId(), item));
        for (DetalleOfertaRequest detalle : request.detalles()) {
            if (!itemsDelLote.containsKey(detalle.itemLoteId())) {
                throw new ReglaNegocioException(
                        "El item " + detalle.itemLoteId() + " no pertenece al lote " + lote.getId());
            }
            if (!ongsPorId.containsKey(detalle.ongId())) {
                throw new ReglaNegocioException(
                        "La ONG " + detalle.ongId() + " no está entre las ONGs seleccionadas");
            }
        }

        Set<String> paresVistos = new HashSet<>();
        for (DetalleOfertaRequest detalle : request.detalles()) {
            String clave = detalle.itemLoteId() + ":" + detalle.ongId();
            if (!paresVistos.add(clave)) {
                throw new ReglaNegocioException(
                        "Hay más de una celda cargada para el mismo item y la misma ONG");
            }
        }

        Map<String, Integer> inventarioPorOngYRecurso = indexarInventario(request.ongIds());
        for (DetalleOfertaRequest detalle : request.detalles()) {
            ItemLote item = itemsDelLote.get(detalle.itemLoteId());
            Ong ong = ongsPorId.get(detalle.ongId());
            Long recursoId = item.getRecurso().getId();
            Integer disponible = inventarioPorOngYRecurso.get(detalle.ongId() + ":" + recursoId);
            if (disponible == null || detalle.cantidadOfrecida() > disponible) {
                int maximo = disponible == null ? 0 : disponible;
                throw new ReglaNegocioException(
                        "La ONG «" + ong.getRazonSocial() + "» tiene " + maximo + " "
                                + item.getRecurso().getUnidadMedida() + " disponibles de "
                                + item.getRecurso().getNombre());
            }
        }

        Set<Long> ongsQueAportan = new HashSet<>();
        request.detalles().forEach(detalle -> ongsQueAportan.add(detalle.ongId()));
        for (Ong ong : ongs) {
            if (!ongsQueAportan.contains(ong.getId())) {
                throw new ReglaNegocioException(
                        "La ONG «" + ong.getRazonSocial() + "» no ofrece ningún recurso");
            }
        }

        Oferta oferta = new Oferta();
        oferta.setEstado(EstadoOferta.PENDIENTE);
        oferta.setFechaOferta(LocalDateTime.now());
        oferta.setLote(lote);
        oferta.setOngs(new HashSet<>(ongs));

        for (DetalleOfertaRequest detalleRequest : request.detalles()) {
            DetalleOferta detalle = new DetalleOferta();
            detalle.setOferta(oferta);
            detalle.setItemLote(itemsDelLote.get(detalleRequest.itemLoteId()));
            detalle.setOng(ongsPorId.get(detalleRequest.ongId()));
            detalle.setCantidadOfrecida(detalleRequest.cantidadOfrecida());
            oferta.getDetalles().add(detalle);
        }

        Oferta guardada = ofertaRepository.save(oferta);
        return ofertaMapper.toResponse(guardada);
    }

    /** Ofertas del lote en las que participa la ONG indicada, de la más nueva a la más vieja. */
    @Transactional(readOnly = true)
    public List<OfertaResponse> listarDeOng(Long loteId, Long ongId) {
        return ofertaRepository.findByLoteIdAndOngId(loteId, ongId).stream()
                .map(ofertaMapper::toResponse)
                .toList();
    }

    private Map<String, Integer> indexarInventario(Set<Long> ongIds) {
        Map<String, Integer> indice = new HashMap<>();
        for (InventarioOng inventario : inventarioOngRepository.findByOngIdIn(ongIds)) {
            String clave = inventario.getOng().getId() + ":" + inventario.getRecurso().getId();
            indice.put(clave, inventario.getCantidadDisponible());
        }
        return indice;
    }
}
