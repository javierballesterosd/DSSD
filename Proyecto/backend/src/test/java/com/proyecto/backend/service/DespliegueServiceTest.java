package com.proyecto.backend.service;

import com.proyecto.backend.client.*;
import com.proyecto.backend.dto.IntegracionNacional;
import com.proyecto.backend.exception.*;
import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class DespliegueServiceTest {
    @Mock EmergenciaRepository emergencias;
    @Mock ActividadRepository actividades;
    @Mock LoteRepository lotes;
    @Mock OfertaRepository ofertas;
    @Mock InventarioOngRepository inventarios;
    @Mock BonitaClient bonita;
    @Mock PlatformTransactionManager tx;
    @Mock org.springframework.context.ApplicationEventPublisher eventos;
    DespliegueService servicio;
    Emergencia emergencia;
    Lote lote;
    final BonitaSession sesion = new BonitaSession("sesion", "token");

    @BeforeEach void preparar() {
        servicio = new DespliegueService(emergencias, actividades, lotes, ofertas, inventarios, bonita, tx, eventos);
        Region region = new Region(); region.setId(3L);
        Municipio municipio = new Municipio(); municipio.setRegion(region);
        emergencia = new Emergencia(); emergencia.setId(7L); emergencia.setBonitaCaseId("44"); emergencia.setMunicipio(municipio);
        lote = new Lote(); lote.setId(8L); lote.setEmergencia(emergencia);
        lote.setDecisionMunicipal(true); lote.setFechaDecisionMunicipal(LocalDateTime.now());
    }
    void prepararTransaccion() {
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(lotes.findFirstByEmergenciaIdOrderByIdDesc(7L)).thenReturn(Optional.of(lote));
    }
    Actividad actividad() {
        Actividad actividad = new Actividad(); actividad.setId(9L); actividad.setEmergencia(emergencia);
        when(actividades.findById(9L)).thenReturn(Optional.of(actividad)); return actividad;
    }
    @Test void unaActividadPendienteMantieneElMonitoreoAbierto() {
        prepararTransaccion(); Actividad actividad = actividad();
        when(actividades.existsByEmergenciaIdAndFinalizadaFalse(7L)).thenReturn(true);
        servicio.finalizarActividad(7L, 9L, 3L, sesion);
        assertThat(actividad.isFinalizada()).isTrue(); verifyNoInteractions(bonita);
        assertThat(emergencia.getFechaFinMonitoreo()).isNull();
    }
    @Test void laUltimaActividadSeConfirmaAntesDeAvisarABonita() {
        prepararTransaccion(); actividad();
        when(bonita.buscarTareaPendiente(sesion, "44", "Monitorear despliegue")).thenReturn("100");
        servicio.finalizarActividad(7L, 9L, 3L, sesion);
        var orden = inOrder(actividades, tx, bonita);
        orden.verify(actividades).saveAndFlush(any());
        orden.verify(tx).commit(any());
        orden.verify(bonita).buscarTareaPendiente(sesion, "44", "Monitorear despliegue");
        verify(bonita).ejecutarTareaIdempotente(sesion, "100", Map.of("hayActividadesPendientes", false));
        assertThat(emergencia.getFechaFinMonitoreo()).isNotNull();
    }
    @Test void reintentarReutilizaLaMismaTareaDespuesDeUnFallo() {
        prepararTransaccion(); emergencia.setTareaMonitoreoBonitaId("100");
        doThrow(new BonitaIntegrationException("Sin conexion")).doNothing().when(bonita)
                .ejecutarTareaIdempotente(sesion, "100", Map.of("hayActividadesPendientes", false));
        assertThatThrownBy(() -> servicio.completarMonitoreo(7L, 3L, sesion)).isInstanceOf(BonitaIntegrationException.class);
        assertThat(emergencia.getFechaFinMonitoreo()).isNull();
        servicio.completarMonitoreo(7L, 3L, sesion);
        verify(bonita, never()).buscarTareaPendiente(any(), anyString(), anyString());
        assertThat(emergencia.getFechaFinMonitoreo()).isNotNull();
    }
    @Test void unCoordinadorAjenoNoPuedeFinalizarActividades() {
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        assertThatThrownBy(() -> servicio.finalizarActividad(7L, 9L, 99L, sesion)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(actividades, bonita);
    }
    Oferta oferta() {
        Oferta oferta = new Oferta(); oferta.setId(20L); oferta.setEstado(EstadoOferta.ADJUDICADA);
        Ong ong = new Ong(); ong.setId(2L); ong.setCodigo("codigo-ong");
        Recurso recurso = new Recurso(); recurso.setId(4L);
        ItemLote item = new ItemLote(); item.setRecurso(recurso);
        DetalleOferta detalle = new DetalleOferta(); detalle.setCantidadAceptada(5); detalle.setOng(ong); detalle.setItemLote(item);
        oferta.getDetalles().add(detalle);
        when(ofertas.findByLoteIdOrderByIdAsc(8L)).thenReturn(List.of(oferta)); return oferta;
    }
    IntegracionNacional.Respuesta recibo(String estado, int cantidad) {
        return new IntegracionNacional.Respuesta(8L, "44", List.of(new IntegracionNacional.Resultado(20L, true,
                List.of(new IntegracionNacional.ItemSN("codigo-ong", 4L, cantidad, estado)))));
    }
    void prepararCierre() {
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(lotes.bloquearPorId(8L)).thenReturn(Optional.of(lote));
        emergencia.setFechaFinMonitoreo(LocalDateTime.now()); emergencia.setFechaValidacionDespliegue(LocalDateTime.now());
    }
    @Test void soloUnaConfirmacionDevuelveElStockYFinalizaElLote() {
        prepararCierre(); Oferta oferta = oferta();
        InventarioOng inventario = new InventarioOng(); inventario.setCantidadDisponible(10);
        when(inventarios.findByOngIdAndRecursoIdForUpdate(2L, 4L)).thenReturn(Optional.of(inventario));
        servicio.confirmar(7L, recibo("LIBERADA", 5));
        servicio.confirmar(7L, recibo("LIBERADA", 5));
        assertThat(inventario.getCantidadDisponible()).isEqualTo(15);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.FINALIZADA);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.FINALIZADO);
        assertThat(emergencia.getFechaFinalizacion()).isNotNull();
        verify(inventarios, times(1)).save(inventario);
        verify(eventos, times(1)).publishEvent(any(DespliegueFinalizadoEvent.class));
    }
    @Test void unaReservaComprometidaNoPermiteDevolverStock() {
        prepararCierre(); oferta();
        assertThatThrownBy(() -> servicio.confirmar(7L, recibo("COMPROMETIDA", 5))).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(inventarios); assertThat(emergencia.getFechaFinalizacion()).isNull();
    }
    @Test void cantidadesInsuficientesNoPermitenDevolverStock() {
        prepararCierre(); oferta();
        assertThatThrownBy(() -> servicio.confirmar(7L, recibo("LIBERADA", 4))).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(inventarios);
    }
    @Test void noSePuedeLiberarSiQuedanActividades() {
        prepararCierre(); when(actividades.existsByEmergenciaIdAndFinalizadaFalse(7L)).thenReturn(true);
        assertThatThrownBy(() -> servicio.confirmar(7L, recibo("LIBERADA", 5))).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(ofertas, inventarios);
    }
    @Test void noSePuedeLiberarAntesDeLaValidacionDelCoordinador() {
        prepararCierre(); emergencia.setFechaValidacionDespliegue(null);
        assertThatThrownBy(() -> servicio.confirmar(7L, recibo("LIBERADA", 5))).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(ofertas, inventarios);
    }
    @Test void guardaDescripcionYCoordinadorAntesDeCompletarLaValidacionEnBonita() {
        prepararTransaccion(); emergencia.setFechaFinMonitoreo(LocalDateTime.now());
        when(bonita.buscarTareaPendiente(sesion, "44", "Validar estado despliegue")).thenReturn("200");
        var usuario = new com.proyecto.backend.dto.auth.LoginResponse();
        usuario.setUserId("30"); usuario.setUsername("coord.norte");
        doAnswer(invocacion -> {
            assertThat(emergencia.getDescripcionCierre()).isEqualTo("Todas las actividades completadas.");
            assertThat(emergencia.getUsuarioCierreId()).isEqualTo("30");
            assertThat(emergencia.getFechaValidacionDespliegue()).isNotNull();
            return null;
        }).when(bonita).ejecutarTareaIdempotente(sesion, "200", Map.of());
        servicio.validarDespliegue(7L, 3L, sesion, " Todas las actividades completadas. ", usuario);
        var orden = inOrder(emergencias, tx, bonita);
        orden.verify(emergencias).save(emergencia);
        orden.verify(tx).commit(any());
        orden.verify(bonita).ejecutarTareaIdempotente(sesion, "200", Map.of());
    }
    @Test void noValidaNiGuardaDescripcionSiHayActividadesPendientes() {
        prepararTransaccion();
        when(actividades.existsByEmergenciaIdAndFinalizadaFalse(7L)).thenReturn(true);
        assertThatThrownBy(() -> servicio.validarDespliegue(7L, 3L, sesion, "Asistencia completada.", null))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(bonita, eventos);
        assertThat(emergencia.getDescripcionCierre()).isNull();
    }
    @Test void unaDescripcionInvalidaNoTieneEfectos() {
        assertThatThrownBy(() -> servicio.validarDespliegue(7L, 3L, sesion, "   ", null))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(emergencias, bonita, eventos);
    }
    @Test void noPermiteCambiarLaDescripcionDeUnCierreSolicitado() {
        prepararTransaccion(); emergencia.setDescripcionCierre("Texto de cierre original.");
        assertThatThrownBy(() -> servicio.validarDespliegue(7L, 3L, sesion, "Una descripcion diferente.", null))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("otra descripción");
        verifyNoInteractions(bonita, eventos);
    }

}
