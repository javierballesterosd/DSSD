package com.proyecto.backend.service;

import com.proyecto.backend.dto.DescriptorAudiencia;
import com.proyecto.backend.dto.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/** Avisa al municipio que ya puede revisar los recursos habilitados y aceptar o rechazar el conjunto. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionOfertasValidadasListener {
    private final NotificacionService notificaciones;
    private final PlatformTransactionManager transactionManager;

    @TransactionalEventListener
    public void alValidarseLasOfertas(OfertasValidadasEvent evento) {
        try {
            var tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(status -> notificaciones.crear("MUNICIPAL",
                    new DescriptorAudiencia(evento.municipioGroupPath()), "Ofertas listas para tu decisión",
                    "El Sistema Nacional habilitó recursos para la emergencia «" + evento.emergenciaTitulo()
                            + "». Revisalos y aceptá o rechazá el conjunto de ofertas.", remitenteSistema()));
        } catch (RuntimeException ex) {
            log.warn("No se pudo avisar al municipio de las ofertas validadas. emergenciaId={}",
                    evento.emergenciaId(), ex);
        }
    }

    // No hay un usuario detrás de este aviso: lo genera el sistema
    private static LoginResponse remitenteSistema() {
        LoginResponse remitente = new LoginResponse();
        remitente.setUserId("sistema");
        remitente.setUsername("Sistema");
        return remitente;
    }
}
