package com.proyecto.backend.service;

import com.proyecto.backend.dto.*;
import com.proyecto.backend.exception.*;
import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** Provee datos y recibe resultados de Bonita; no llama al Sistema Nacional. */
@Service
@RequiredArgsConstructor
public class ValidacionNacionalService {
    private final LoteRepository lotes;
    private final OfertaRepository ofertas;
    private final OfertaService ofertaService;

    @Transactional(readOnly = true)
    public IntegracionNacional.Plan plan(Long emergenciaId, Long loteId, String caseId) {
        Lote lote = comprobar(emergenciaId, loteId, caseId);
        if (lote.getFechaCierreOfertas() == null || lote.getFechaCierreOfertas().isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La convocatoria debe cerrar antes de validar las ofertas");
        }
        boolean decision = lote.getDecisionMunicipal() != null;
        if (lote.getEstado() != EstadoLote.ACTIVO && lote.getFechaDecisionMunicipal() == null) {
            throw new ReglaNegocioException("El lote no está activo");
        }
        var pendientes = ofertas.findByLoteIdOrderByIdAsc(loteId).stream()
                .filter(o -> decision ? o.getEstado() == EstadoOferta.VALIDADA || o.getEstado() == EstadoOferta.ACEPTADA_PARCIAL
                        : o.getEstado() == EstadoOferta.PENDIENTE)
                .map(o -> new IntegracionNacional.Oferta(o.getId(), o.getDetalles().stream()
                        .filter(d -> !decision || d.getCantidadAceptada() != null && d.getCantidadAceptada() > 0)
                        .map(d -> new IntegracionNacional.Item(d.getId(), d.getOng().getCodigo(),
                                d.getItemLote().getRecurso().getId(), decision ? d.getCantidadAceptada() : d.getCantidadOfrecida()))
                        .toList())).toList();
        return new IntegracionNacional.Plan(emergenciaId, loteId, caseId, lote.getDecisionMunicipal(),
                lote.getFechaDecisionMunicipal() != null, pendientes);
    }

    @Transactional
    public void registrar(Long emergenciaId, IntegracionNacional.Respuesta respuesta) {
        // Tomar el lock antes de leer el estado que decide si el callback sigue vigente.
        lotes.bloquearPorId(respuesta.loteId()).orElseThrow();
        Lote lote = comprobar(emergenciaId, respuesta.loteId(), respuesta.caseId());
        if (lote.getEstado() != EstadoLote.ACTIVO || lote.getDecisionMunicipal() != null
                || lote.getFechaCierreOfertas() == null || lote.getFechaCierreOfertas().isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("El lote ya no admite validaciones nacionales");
        }
        Set<Long> recibidas = new HashSet<>();
        for (var resultado : respuesta.resultados()) {
            if (!recibidas.add(resultado.ofertaId())) throw new ReglaNegocioException("Oferta repetida");
            Oferta oferta = ofertas.findById(resultado.ofertaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la oferta"));
            if (!oferta.getLote().getId().equals(lote.getId())) throw new ReglaNegocioException("Oferta de otro lote");
            Map<String, Integer> cantidades = new HashMap<>();
            for (var item : resultado.items()) {
                if (!resultado.aprobada() || !"BLOQUEADA".equals(item.estado()) || item.cantidad() <= 0) {
                    throw new ReglaNegocioException("Reserva temporal nacional inválida");
                }
                cantidades.merge(item.ongCodigo() + ":" + item.recursoId(), item.cantidad(), Math::addExact);
            }
            var items = oferta.getDetalles().stream().map(d -> {
                Integer cantidad = cantidades.remove(d.getOng().getCodigo() + ":" + d.getItemLote().getRecurso().getId());
                return new ItemValidadoRequest(d.getId(), cantidad == null ? 0 : cantidad);
            }).toList();
            if (!cantidades.isEmpty()) throw new ReglaNegocioException("Recursos ajenos a la oferta");
            ofertaService.procesarValidacionSNYBloquearRecursos(oferta.getId(), new ValidacionSNRequest(
                    resultado.aprobada(), "Resultado nacional recibido desde Bonita", resultado.aprobada() ? items : List.of()));
        }
    }

    private Lote comprobar(Long emergenciaId, Long loteId, String caseId) {
        Lote lote = lotes.findById(loteId).orElseThrow(() -> new RecursoNoEncontradoException("No existe el lote"));
        if (!lote.getEmergencia().getId().equals(emergenciaId)
                || !Objects.equals(lote.getEmergencia().getBonitaCaseId(), caseId)) {
            throw new ReglaNegocioException("El lote o el caso no corresponde a la emergencia");
        }
        return lote;
    }
}
