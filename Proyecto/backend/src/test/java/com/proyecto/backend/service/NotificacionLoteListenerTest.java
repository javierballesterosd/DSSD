package com.proyecto.backend.service;

import com.proyecto.backend.dto.DescriptorAudiencia;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.model.EstadoLote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionLoteListenerTest {

    @Mock
    private NotificacionService notificacionService;
    @InjectMocks
    private NotificacionLoteListener listener;

    private final LoginResponse remitente = new LoginResponse();

    private LoteCambiadoEvent evento(EstadoLote estado) {
        return new LoteCambiadoEvent(
                30L, "Asistencia alimentaria", estado,
                LocalDateTime.of(2026, 10, 1, 18, 0), LocalDateTime.of(2026, 10, 3, 18, 0),
                20L, "Zona Norte", "/Municipio/Region1/LaPlata", remitente);
    }

    @Test
    void alPublicarseElLoteNotificaALosOperadoresDelMunicipio() {
        listener.alCambiarLote(evento(EstadoLote.ACTIVO));

        ArgumentCaptor<DescriptorAudiencia> audiencia = ArgumentCaptor.forClass(DescriptorAudiencia.class);
        ArgumentCaptor<String> descripcion = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).crear(
                eq("MUNICIPAL"), audiencia.capture(),
                eq("Lote publicado para tu emergencia"), descripcion.capture(), eq(remitente));
        assertThat(audiencia.getValue().grupoDestinatario()).isEqualTo("/Municipio/Region1/LaPlata");
        assertThat(descripcion.getValue())
                .contains("«Asistencia alimentaria»")
                .contains("emergencia #20 (Zona Norte)")
                .contains("del 01/10/2026 18:00hs al 03/10/2026 18:00hs");
    }

    @Test
    void elTituloReflejaElEstadoEnQueQuedoElLote() {
        listener.alCambiarLote(evento(EstadoLote.CANCELADO));

        verify(notificacionService).crear(
                eq("MUNICIPAL"), any(DescriptorAudiencia.class),
                eq("Lote cancelado en tu emergencia"), any(String.class), eq(remitente));
    }

    @Test
    void siFallaLaNotificacionNoPropagaElError() {
        when(notificacionService.crear(
                any(String.class), any(DescriptorAudiencia.class),
                any(String.class), any(String.class), any(LoginResponse.class)))
                .thenThrow(new RuntimeException("fallo de persistencia"));

        assertThatCode(() -> listener.alCambiarLote(evento(EstadoLote.ACTIVO))).doesNotThrowAnyException();
    }
}
