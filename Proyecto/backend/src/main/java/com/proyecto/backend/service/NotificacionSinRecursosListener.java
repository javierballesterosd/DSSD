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

/** Avisa al municipio que debe volver a registrar la emergencia y a las ONGs que sus ofertas no se habilitaron. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionSinRecursosListener {
    private final NotificacionService notificaciones;
    private final PlatformTransactionManager transactionManager;

    @TransactionalEventListener
    public void alQuedarSinRecursos(SinRecursosEvent evento) {
        LoginResponse remitente = remitenteSistema();
        crear("MUNICIPAL", evento.municipioGroupPath(), "Debés volver a registrar la emergencia",
                "El Sistema Nacional no pudo habilitar recursos para ninguna oferta de la emergencia «"
                        + evento.emergenciaTitulo() + "». Registrala de nuevo (podés mantener los mismos datos) "
                        + "para reiniciar la convocatoria.", remitente, evento);
        for (String path : evento.ongGroupPaths()) {
            crear("ONG", path, "Tus ofertas no fueron habilitadas",
                    "El Sistema Nacional no habilitó recursos para las ofertas de la emergencia «"
                            + evento.emergenciaTitulo() + "». No se reservó nada de tu inventario.", remitente, evento);
        }
    }

    // No hay un usuario detrás de este aviso: lo genera el sistema
    private static LoginResponse remitenteSistema() {
        LoginResponse remitente = new LoginResponse();
        remitente.setUserId("sistema");
        remitente.setUsername("Sistema");
        return remitente;
    }

    private void crear(String rol, String path, String titulo, String texto, LoginResponse remitente,
            SinRecursosEvent evento) {
        try {
            var tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(status ->
                    notificaciones.crear(rol, new DescriptorAudiencia(path), titulo, texto, remitente));
        } catch (RuntimeException ex) {
            log.warn("No se pudo notificar la emergencia sin recursos. emergenciaId={}, grupo={}",
                    evento.emergenciaId(), path, ex);
        }
    }
}
