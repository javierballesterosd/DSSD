package com.proyecto.backend.service;

import com.proyecto.backend.client.SistemaNacionalClient;
import com.proyecto.backend.dto.ItemValidadoRequest;
import com.proyecto.backend.dto.ValidacionSNRequest;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.EstadoOferta;
import com.proyecto.backend.model.Oferta;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.OfertaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reserva en SN y aplica el resultado local después del envío de ofertas desde Bonita. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidacionNacionalService {
    private final LoteRepository lotes;
    private final OfertaRepository ofertas;
    private final EmergenciaService emergencias;
    private final OfertaService ofertaService;
    private final SistemaNacionalClient nacional;

    @Transactional
    public void validar(Long emergenciaId) {
        validar(emergenciaId, false);
    }

    @Transactional
    public void validar(Long emergenciaId, boolean ofertasEnviadasPorBonita) {
        var lote = lotes.bloquearUltimoDeEmergencia(emergenciaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un lote para la emergencia"));
        if (lote.getEstado() != EstadoLote.ACTIVO || lote.getDecisionMunicipal() != null) {
            throw new ReglaNegocioException("El lote ya no admite validaciones nacionales");
        }
        if (lote.getFechaCierreOfertas() == null || lote.getFechaCierreOfertas().isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La convocatoria debe cerrar antes de validar las ofertas");
        }
        List<Oferta> pendientes = ofertas.findByLoteIdOrderByIdAsc(lote.getId()).stream()
                .filter(o -> o.getEstado() == EstadoOferta.PENDIENTE).toList();
        if (pendientes.isEmpty()) return;
        String token = nacional.autenticar();
        // Compatibilidad con casos ya iniciados con el proceso 1.9.
        // Desde 1.9.1 Bonita obtiene el JWT y envía el JSON directamente al SN.
        if (!ofertasEnviadasPorBonita) {
            nacional.enviarOfertas(token, emergencias.obtenerOfertasConsolidadas(emergenciaId));
        }
        for (Oferta oferta : pendientes) {
            var reserva = nacional.reservarTemporal(token,
                    new SistemaNacionalClient.Reserva(emergenciaId, lote.getId(), oferta.getId()));
            if (reserva.isEmpty()) {
                ofertaService.procesarValidacionSNYBloquearRecursos(oferta.getId(),
                        new ValidacionSNRequest(false, "El SN no pudo reservar los recursos", List.of()));
                continue;
            }
            Map<String, Integer> cantidades = new HashMap<>();
            for (var item : reserva.get().items()) {
                if (!"BLOQUEADA".equals(item.estado()) || item.cantidad() == null || item.cantidad() <= 0) {
                    throw new ReglaNegocioException("El SN devolvió una reserva temporal inválida");
                }
                cantidades.merge(item.ongId() + ":" + item.recursoId(), item.cantidad(), Math::addExact);
            }
            var items = oferta.getDetalles().stream().map(d -> {
                String clave = d.getOng().getId() + ":" + d.getItemLote().getRecurso().getId();
                Integer cantidad = cantidades.remove(clave);
                return new ItemValidadoRequest(d.getId(), cantidad == null ? 0 : cantidad);
            }).toList();
            if (!cantidades.isEmpty()) throw new ReglaNegocioException("SN devolvió recursos ajenos a la oferta");
            ofertaService.procesarValidacionSNYBloquearRecursos(oferta.getId(),
                    new ValidacionSNRequest(true, "Reserva temporal confirmada por SN", items));
        }
        log.info("Validación nacional terminada. emergenciaId={}, loteId={}, ofertas={}",
                emergenciaId, lote.getId(), pendientes.size());
    }
}
