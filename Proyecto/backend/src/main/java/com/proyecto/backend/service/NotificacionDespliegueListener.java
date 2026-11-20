package com.proyecto.backend.service;

import com.proyecto.backend.dto.DescriptorAudiencia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionDespliegueListener {
    private final NotificacionService notificaciones;
    private final PlatformTransactionManager transactionManager;

    @TransactionalEventListener
    public void alFinalizar(DespliegueFinalizadoEvent evento) {
        evento.ongGroupPaths().forEach(path -> crear("ONG", path, evento));
        crear("MUNICIPAL", evento.municipioGroupPath(), evento);
    }

    private void crear(String rol, String path, DespliegueFinalizadoEvent evento) {
        try {
            var tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            String texto = "Finalizó la emergencia #" + evento.emergenciaId()
                    + " y se liberaron los recursos. " + evento.descripcionCierre();
            String resumen = texto.length() <= 1000 ? texto : texto.substring(0, 997) + "...";
            tx.executeWithoutResult(status -> notificaciones.crear(rol, new DescriptorAudiencia(path),
                    "Despliegue finalizado", resumen, evento.remitente()));
        } catch (RuntimeException ex) {
            log.warn("No se pudo notificar el cierre. emergenciaId={}, grupo={}", evento.emergenciaId(), path, ex);
        }
    }
}
