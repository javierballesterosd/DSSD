package com.proyecto.backend.service;

import com.proyecto.backend.dto.DescriptorAudiencia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifica a los coordinadores de la región cuando se registra una emergencia. Es una ayuda para el
 * usuario, no algo esencial: corre después del commit, en su propia transacción, y si falla solo se
 * registra en el log (nunca afecta la emergencia ni se le muestra al usuario).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionEmergenciaListener {

    private final NotificacionService notificacionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alRegistrarseEmergencia(EmergenciaRegistradaEvent evento) {
        try {
            String descripcion = String.format(
                    "Se ha registrado una nueva emergencia en el municipio de %s. "
                            + "Nivel de gravedad: %s. Zona afectada: %s.",
                    evento.municipioNombre(), evento.nivelGravedad(), evento.zonaAfectada());

            notificacionService.crear(
                    "COORDINADOR",
                    new DescriptorAudiencia(evento.regionGroupPath()),
                    "Nueva emergencia registrada",
                    descripcion,
                    evento.remitente());
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la notificación de la emergencia. emergenciaId={}",
                    evento.emergenciaId(), e);
        }
    }
}
