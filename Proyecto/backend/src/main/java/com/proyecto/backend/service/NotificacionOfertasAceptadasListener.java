package com.proyecto.backend.service;

import com.proyecto.backend.dto.DescriptorAudiencia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionOfertasAceptadasListener {

    private final NotificacionService notificacionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alAceptarseOferta(OfertaAceptadaEvent evento) {
        String descripcion = String.format(
                "El municipio de %s aceptó la oferta #%d para la emergencia en «%s». Los recursos ya fueron bloqueados y la oferta está en ejecución.",
                evento.municipioNombre(), evento.ofertaId(), evento.zonaAfectada());

        for (String groupPath : evento.ongsGroupPaths()) {
            try {
                notificacionService.crear(
                        "ONG", // Verificá que coincida con el nombre exacto del rol en tu sistema
                        new DescriptorAudiencia(groupPath),
                        "Oferta aceptada por el municipio",
                        descripcion,
                        evento.remitente());
            } catch (RuntimeException e) {
                log.warn("No se pudo crear la notificación para la ONG. ofertaId={}, groupPath={}",
                        evento.ofertaId(), groupPath, e);
            }
        }
    }
}
