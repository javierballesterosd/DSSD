package com.proyecto.backend.service;

import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.DescriptorAudiencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionEmergenciaListenerTest {

    @Mock
    private NotificacionService notificacionService;
    @InjectMocks
    private NotificacionEmergenciaListener listener;

    private final LoginResponse remitente = new LoginResponse();

    private EmergenciaRegistradaEvent evento() {
        return new EmergenciaRegistradaEvent(
                20L, "La Plata", "ALTA", "Zona Norte", "/Municipio/Region1", remitente);
    }

    @Test
    void creaLaNotificacionParaLosCoordinadoresDeLaRegion() {
        listener.alRegistrarseEmergencia(evento());

        ArgumentCaptor<DescriptorAudiencia> audiencia = ArgumentCaptor.forClass(DescriptorAudiencia.class);
        verify(notificacionService).crear(
                eq("COORDINADOR"), audiencia.capture(),
                eq("Nueva emergencia registrada"), any(String.class), eq(remitente));
        assertThat(audiencia.getValue().grupoDestinatario()).isEqualTo("/Municipio/Region1");
    }

    @Test
    void siFallaLaNotificacionNoPropagaElError() {
        when(notificacionService.crear(
                any(String.class), any(DescriptorAudiencia.class),
                any(String.class), any(String.class), any(LoginResponse.class)))
                .thenThrow(new RuntimeException("fallo de persistencia"));

        assertThatCode(() -> listener.alRegistrarseEmergencia(evento())).doesNotThrowAnyException();
    }
}
