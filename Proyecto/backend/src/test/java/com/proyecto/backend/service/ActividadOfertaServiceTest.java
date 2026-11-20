package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.ActividadOfertaRepository;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.OfertaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActividadOfertaServiceTest {
    @Mock ActividadOfertaRepository avances;
    @Mock OfertaRepository ofertas;
    @Mock EmergenciaRepository emergencias;
    @Mock DespliegueService despliegue;
    @Mock PlatformTransactionManager tx;
    ActividadOfertaService servicio;
    Emergencia emergencia;
    Oferta oferta;
    Ong cruzRoja;
    Ong caritas;
    ActividadOferta avance;
    final BonitaSession sesion = new BonitaSession("sesion", "token");

    @BeforeEach void preparar() {
        servicio = new ActividadOfertaService(avances, ofertas, emergencias, despliegue, tx);
        emergencia = new Emergencia(); emergencia.setId(7L);
        var municipio = new Municipio(); municipio.setNombre("La Plata");
        emergencia.setMunicipio(municipio); emergencia.setNivelGravedad(NivelGravedad.ALTA);
        emergencia.setZonaAfectada("Plaza Moreno");
        var lote = new Lote(); lote.setId(8L); lote.setTitulo("Lote 1"); lote.setEmergencia(emergencia);
        cruzRoja = new Ong(); cruzRoja.setId(1L); cruzRoja.setRazonSocial("Cruz Roja");
        caritas = new Ong(); caritas.setId(2L); caritas.setRazonSocial("Cáritas");
        oferta = new Oferta(); oferta.setId(20L); oferta.setLote(lote); oferta.setEstado(EstadoOferta.ADJUDICADA);
        oferta.setOngs(Set.of(cruzRoja, caritas));
        var actividad = new Actividad(); actividad.setId(9L); actividad.setNombre("Evacuación");
        actividad.setEmergencia(emergencia);
        avance = new ActividadOferta(); avance.setActividad(actividad); avance.setOferta(oferta);
    }

    void prepararFinalizacion() {
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(ofertas.findById(20L)).thenReturn(Optional.of(oferta));
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
    }

    @Test void unaOngDelConsorcioMarcaLaActividadParaTodasYLaOfertaPasaAEnEjecucion() {
        prepararFinalizacion();
        when(avances.findByActividadIdAndOfertaId(9L, 20L)).thenReturn(Optional.of(avance));

        var respuesta = servicio.finalizar(20L, 9L, 1L, "ong.cruzroja", sesion);

        assertThat(avance.isFinalizada()).isTrue();
        assertThat(avance.getOngFinalizacion()).isSameAs(cruzRoja);
        assertThat(avance.getUsuarioFinalizacion()).isEqualTo("ong.cruzroja");
        assertThat(avance.getFechaFinalizacion()).isNotNull();
        assertThat(respuesta.finalizada()).isTrue();
        assertThat(respuesta.finalizadaPor()).isEqualTo("Cruz Roja");
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.EN_EJECUCION);
        verify(despliegue).verificarCierre(7L, sesion);
    }

    @Test void repetirLaMarcaNoCambiaNadaYLaOtraOngYaLaVeHecha() {
        prepararFinalizacion();
        when(avances.findByActividadIdAndOfertaId(9L, 20L)).thenReturn(Optional.of(avance));
        servicio.finalizar(20L, 9L, 1L, "ong.cruzroja", sesion);
        var fecha = avance.getFechaFinalizacion();

        var respuesta = servicio.finalizar(20L, 9L, 2L, "ong.caritas", sesion);

        assertThat(respuesta.finalizadaPor()).isEqualTo("Cruz Roja");
        assertThat(avance.getFechaFinalizacion()).isEqualTo(fecha);
        assertThat(avance.getUsuarioFinalizacion()).isEqualTo("ong.cruzroja");
        verify(avances, times(1)).saveAndFlush(avance);
    }

    @Test void unaOngAjenaALaOfertaNoPuedeMarcarActividades() {
        prepararFinalizacion();
        assertThatThrownBy(() -> servicio.finalizar(20L, 9L, 99L, "ong.otra", sesion))
                .isInstanceOf(AccesoDenegadoException.class);
        assertThat(avance.isFinalizada()).isFalse();
        verifyNoInteractions(despliegue);
    }

    @Test void unaOfertaQueNoEstaAdjudicadaNoAdmiteActividades() {
        prepararFinalizacion(); oferta.setEstado(EstadoOferta.NO_ADJUDICADA);
        assertThatThrownBy(() -> servicio.finalizar(20L, 9L, 1L, "ong.cruzroja", sesion))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(despliegue);
    }

    @Test void siBonitaFallaLaMarcaQuedaIgualConfirmada() {
        prepararFinalizacion();
        when(avances.findByActividadIdAndOfertaId(9L, 20L)).thenReturn(Optional.of(avance));
        doThrow(new RuntimeException("Bonita caído")).when(despliegue).verificarCierre(7L, sesion);

        var respuesta = servicio.finalizar(20L, 9L, 1L, "ong.cruzroja", sesion);

        assertThat(respuesta.finalizada()).isTrue();
        assertThat(avance.isFinalizada()).isTrue();
    }

    @Test void listaLasOfertasEnCursoConElProgresoDeSusActividades() {
        avance.setFinalizada(true); avance.setOngFinalizacion(caritas);
        var otra = new Actividad(); otra.setId(10L); otra.setNombre("Reparto");
        var pendiente = new ActividadOferta(); pendiente.setActividad(otra); pendiente.setOferta(oferta);
        when(avances.findDeOng(eq(1L), any())).thenReturn(List.of(avance, pendiente));

        var despliegues = servicio.misDespliegues(1L);

        assertThat(despliegues).hasSize(1);
        var d = despliegues.getFirst();
        assertThat(d.ongs()).containsExactlyInAnyOrder("Cáritas", "Cruz Roja");
        assertThat(d.totalActividades()).isEqualTo(2);
        assertThat(d.actividadesFinalizadas()).isEqualTo(1);
        assertThat(d.emergenciaZona()).isEqualTo("Plaza Moreno");
        assertThat(d.actividades().getFirst().finalizadaPor()).isEqualTo("Cáritas");
        assertThat(d.actividades().get(1).finalizada()).isFalse();
    }
}
