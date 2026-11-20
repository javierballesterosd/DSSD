package com.proyecto.backend.service;

import com.proyecto.backend.dto.DescriptorAudiencia;
import com.proyecto.backend.dto.auth.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NotificacionDespliegueListenerTest {
    @Test void notificaAlMunicipioYTodasLasOngParticipantesConDescripcionAcotada() {
        var notificaciones = mock(NotificacionService.class);
        var tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var remitente = new LoginResponse(); remitente.setUserId("30");
        var evento = new DespliegueFinalizadoEvent(7L, "/Municipio/LaPlata", Set.of("/ONG/A", "/ONG/B"),
                "Asistencia completada. " + "x".repeat(2000), remitente);
        new NotificacionDespliegueListener(notificaciones, tx).alFinalizar(evento);
        verify(notificaciones).crear(eq("ONG"), eq(new DescriptorAudiencia("/ONG/A")), eq("Despliegue finalizado"),
                argThat(s -> s.length() <= 1000 && s.contains("Asistencia completada")), eq(remitente));
        verify(notificaciones).crear(eq("ONG"), eq(new DescriptorAudiencia("/ONG/B")), anyString(), anyString(), eq(remitente));
        verify(notificaciones).crear(eq("MUNICIPAL"), eq(new DescriptorAudiencia("/Municipio/LaPlata")), anyString(), anyString(), eq(remitente));
        verify(tx, times(3)).commit(any());
    }

    @Test void unaNotificacionFallidaNoImpideNotificarAlMunicipio() {
        var notificaciones = mock(NotificacionService.class);
        var tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var remitente = new LoginResponse(); remitente.setUserId("30");
        when(notificaciones.crear(eq("ONG"), any(), anyString(), anyString(), eq(remitente)))
                .thenThrow(new IllegalStateException("Sin conexión"));
        new NotificacionDespliegueListener(notificaciones, tx).alFinalizar(new DespliegueFinalizadoEvent(7L,
                "/Municipio/LaPlata", Set.of("/ONG/A"), "Asistencia finalizada.", remitente));
        verify(notificaciones).crear(eq("MUNICIPAL"), any(), anyString(), anyString(), eq(remitente));
    }
}
