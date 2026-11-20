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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionesConNombreDeEmergenciaTest {

    @Mock
    private NotificacionService notificaciones;
    @Mock
    private PlatformTransactionManager transacciones;

    @BeforeEach
    void preparar() {
        when(transacciones.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
    }

    @Test
    void elMunicipioRecibeUnAvisoParaDecidirConElNombreDeLaEmergencia() {
        var listener = new NotificacionOfertasValidadasListener(notificaciones, transacciones);

        listener.alValidarseLasOfertas(new OfertasValidadasEvent(7L, "Plaza Moreno", "/Municipio/Region1/LaPlata"));

        ArgumentCaptor<DescriptorAudiencia> audiencia = ArgumentCaptor.forClass(DescriptorAudiencia.class);
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(notificaciones).crear(eq("MUNICIPAL"), audiencia.capture(), eq("Ofertas listas para tu decisión"),
                texto.capture(), any(LoginResponse.class));
        assertThat(audiencia.getValue().grupoDestinatario()).isEqualTo("/Municipio/Region1/LaPlata");
        assertThat(texto.getValue()).contains("«Plaza Moreno»").doesNotContain("#7");
    }

    @Test
    void siFallaElAvisoNoPropagaElError() {
        when(notificaciones.crear(any(String.class), any(DescriptorAudiencia.class), any(String.class),
                any(String.class), any(LoginResponse.class))).thenThrow(new RuntimeException("fallo"));
        var listener = new NotificacionOfertasValidadasListener(notificaciones, transacciones);

        assertThatCode(() -> listener.alValidarseLasOfertas(new OfertasValidadasEvent(7L, "Plaza Moreno", "/Municipio/x")))
                .doesNotThrowAnyException();
    }

    @Test
    void laDecisionDelMunicipioNombraLaEmergenciaPorSuTitulo() {
        var listener = new NotificacionDecisionMunicipalListener(notificaciones, transacciones);
        var remitente = new LoginResponse();

        listener.alDecidir(new DecisionMunicipalEvent(7L, "Plaza Moreno", false, "/Municipio/Region1/LaPlata",
                Set.of("/ONG/Caritas"), remitente));

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(notificaciones).crear(eq("ONG"), any(DescriptorAudiencia.class), any(String.class), texto.capture(),
                eq(remitente));
        assertThat(texto.getValue()).contains("«Plaza Moreno»").doesNotContain("#7");
        verify(notificaciones).crear(eq("MUNICIPAL"), any(DescriptorAudiencia.class), eq("Debés actualizar la emergencia"),
                texto.capture(), eq(remitente));
        assertThat(texto.getValue()).contains("«Plaza Moreno»").doesNotContain("#7");
    }

    @Test
    void elCierreSinRecursosNombraLaEmergenciaPorSuTitulo() {
        var listener = new NotificacionSinRecursosListener(notificaciones, transacciones);

        listener.alQuedarSinRecursos(new SinRecursosEvent(7L, "Plaza Moreno", "/Municipio/Region1/LaPlata",
                Set.of("/ONG/Caritas")));

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(notificaciones).crear(eq("MUNICIPAL"), any(DescriptorAudiencia.class), any(String.class),
                texto.capture(), any(LoginResponse.class));
        assertThat(texto.getValue()).contains("«Plaza Moreno»").doesNotContain("#7");
    }
}
