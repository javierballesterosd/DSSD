package com.proyecto.backend.service;

import com.proyecto.backend.dto.DescriptorAudiencia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionDecisionMunicipalListener {
    private final NotificacionService notificaciones;
    private final PlatformTransactionManager transactionManager;

    @TransactionalEventListener
    public void alDecidir(DecisionMunicipalEvent evento) {
        for (String path : evento.ongGroupPaths()) {
            crear("ONG", path, evento.aceptada() ? "Ofertas aceptadas por el municipio" : "Ofertas rechazadas por el municipio",
                    "El municipio " + (evento.aceptada() ? "aceptó los recursos habilitados" : "rechazó el conjunto de ofertas; se liberaron los recursos")
                            + " de la emergencia #" + evento.emergenciaId() + ".", evento);
        }
        if (!evento.aceptada()) {
            crear("MUNICIPAL", evento.municipioGroupPath(), "Debés actualizar la emergencia",
                    "Actualizá la emergencia #" + evento.emergenciaId()
                            + " para reiniciar la convocatoria. Podés confirmar los mismos datos.", evento);
        }
    }

    private void crear(String rol, String path, String titulo, String texto, DecisionMunicipalEvent evento) {
        try {
            var tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(status ->
                    notificaciones.crear(rol, new DescriptorAudiencia(path), titulo, texto, evento.remitente()));
        } catch (RuntimeException ex) {
            log.warn("No se pudo notificar la decisión municipal. emergenciaId={}, grupo={}", evento.emergenciaId(), path, ex);
        }
    }
}
