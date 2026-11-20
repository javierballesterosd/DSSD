package com.proyecto.backend.service;

import com.proyecto.backend.dto.DescriptorAudiencia;
import com.proyecto.backend.dto.auth.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionSinRecursosListenerTest {

    @Mock
    private NotificacionService notificaciones;
    @Mock
    private PlatformTransactionManager transacciones;
    private NotificacionSinRecursosListener listener;

    @BeforeEach
    void preparar() {
        listener = new NotificacionSinRecursosListener(notificaciones, transacciones);
        when(transacciones.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
    }

    private SinRecursosEvent evento() {
        return new SinRecursosEvent(7L, "Plaza Moreno", "/Municipio/Region1/LaPlata", Set.of("/ONG/Caritas", "/ONG/Bomberos"));
    }

    @Test
    void avisaAlMunicipioQueDebeVolverARegistrarYAVisaALasOngs() {
        listener.alQuedarSinRecursos(evento());

        ArgumentCaptor<DescriptorAudiencia> audiencia = ArgumentCaptor.forClass(DescriptorAudiencia.class);
        ArgumentCaptor<LoginResponse> remitente = ArgumentCaptor.forClass(LoginResponse.class);
        verify(notificaciones).crear(eq("MUNICIPAL"), audiencia.capture(), eq("Debés volver a registrar la emergencia"),
                any(String.class), remitente.capture());
        assertThat(audiencia.getValue().grupoDestinatario()).isEqualTo("/Municipio/Region1/LaPlata");
        assertThat(remitente.getValue().getUserId()).isEqualTo("sistema");
        verify(notificaciones, times(2)).crear(eq("ONG"), any(DescriptorAudiencia.class),
                eq("Tus ofertas no fueron habilitadas"), any(String.class), any(LoginResponse.class));
    }

    @Test
    void siFallaUnaNotificacionNoPropagaElError() {
        when(notificaciones.crear(any(String.class), any(DescriptorAudiencia.class), any(String.class),
                any(String.class), any(LoginResponse.class))).thenThrow(new RuntimeException("fallo"));

        assertThatCode(() -> listener.alQuedarSinRecursos(evento())).doesNotThrowAnyException();
    }
}
