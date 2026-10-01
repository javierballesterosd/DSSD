package com.proyecto.backend.service;

import com.proyecto.backend.dto.DescriptorAudiencia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Notifica a los operadores del municipio cuando cambia el lote de una de sus emergencias. Igual que
 * la notificación de emergencias, es una ayuda y no algo esencial: corre después del commit, en su
 * propia transacción, y si falla solo se registra en el log.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionLoteListener {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm'hs'");

    private final NotificacionService notificacionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCambiarLote(LoteCambiadoEvent evento) {
        try {
            notificacionService.crear(
                    "MUNICIPAL",
                    new DescriptorAudiencia(evento.municipioGroupPath()),
                    titulo(evento),
                    descripcion(evento),
                    evento.remitente());
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la notificación del lote. loteId={}, emergenciaId={}",
                    evento.loteId(), evento.emergenciaId(), e);
        }
    }

    private String titulo(LoteCambiadoEvent evento) {
        return switch (evento.estado()) {
            case ACTIVO -> "Lote publicado para tu emergencia";
            case CANCELADO -> "Lote cancelado en tu emergencia";
            case FINALIZADO -> "Lote finalizado en tu emergencia";
        };
    }

    private String descripcion(LoteCambiadoEvent evento) {
        String emergencia = String.format("la emergencia #%d (%s)", evento.emergenciaId(), evento.zonaAfectada());
        return switch (evento.estado()) {
            case ACTIVO -> String.format(
                    "El Centro Coordinador Regional publicó el lote «%s» para %s. "
                            + "Recepción de ofertas: del %s al %s.",
                    evento.loteTitulo(), emergencia,
                    fecha(evento.fechaAperturaOfertas()), fecha(evento.fechaCierreOfertas()));
            case CANCELADO -> String.format(
                    "El Centro Coordinador Regional canceló el lote «%s» de %s.",
                    evento.loteTitulo(), emergencia);
            case FINALIZADO -> String.format(
                    "Finalizaron las actividades del lote «%s» de %s.",
                    evento.loteTitulo(), emergencia);
        };
    }

    private String fecha(LocalDateTime fecha) {
        return fecha != null ? fecha.format(FECHA) : "sin definir";
    }
}
