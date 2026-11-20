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
    @Mock ActividadOfertaRepository asignaciones;
    @Mock LoteRepository lotes;
    @Mock OfertaRepository ofertas;
    @Mock InventarioOngRepository inventarios;
    @Mock BonitaClient bonita;
    @Mock PlatformTransactionManager tx;
    DespliegueService servicio;
    Emergencia emergencia;
    Lote lote;
    final BonitaSession sesion = new BonitaSession("sesion", "token");

    @BeforeEach void preparar() {
        servicio = new DespliegueService(emergencias, asignaciones, lotes, ofertas, inventarios, bonita, tx);
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
    /** Hay actividades asignadas a las ofertas adjudicadas y ninguna quedó pendiente. */
    void todoFinalizado() {
        when(asignaciones.existsByActividadEmergenciaId(7L)).thenReturn(true);
    }
    @Test void unaActividadPendienteMantieneElMonitoreoAbierto() {
        prepararTransaccion(); todoFinalizado();
        when(asignaciones.existsByActividadEmergenciaIdAndFinalizadaFalse(7L)).thenReturn(true);
        servicio.verificarCierre(7L, sesion);
        verifyNoInteractions(bonita);
        assertThat(emergencia.getFechaFinMonitoreo()).isNull();
    }
    @Test void sinActividadesAsignadasElMonitoreoNoSeCierra() {
        prepararTransaccion();
        servicio.verificarCierre(7L, sesion);
        verifyNoInteractions(bonita);
        assertThat(emergencia.getFechaFinMonitoreo()).isNull();
    }
    @Test void laUltimaActividadCierraElMonitoreoSinExigirCoordinador() {
        prepararTransaccion(); todoFinalizado();
        when(bonita.buscarTareaPendiente(sesion, "44", "Monitorear despliegue")).thenReturn("100");
        servicio.verificarCierre(7L, sesion);
        verify(bonita).ejecutarTareaIdempotente(sesion, "100", Map.of("hayActividadesPendientes", false));
        assertThat(emergencia.getFechaFinMonitoreo()).isNotNull();
    }
    @Test void reintentarReutilizaLaMismaTareaDespuesDeUnFallo() {
        prepararTransaccion(); todoFinalizado(); emergencia.setTareaMonitoreoBonitaId("100");
        doThrow(new BonitaIntegrationException("Sin conexion")).doNothing().when(bonita)
                .ejecutarTareaIdempotente(sesion, "100", Map.of("hayActividadesPendientes", false));
        assertThatThrownBy(() -> servicio.completarMonitoreo(7L, 3L, sesion)).isInstanceOf(BonitaIntegrationException.class);
        assertThat(emergencia.getFechaFinMonitoreo()).isNull();
        servicio.completarMonitoreo(7L, 3L, sesion);
        verify(bonita, never()).buscarTareaPendiente(any(), anyString(), anyString());
        assertThat(emergencia.getFechaFinMonitoreo()).isNotNull();
    }
    @Test void unCoordinadorAjenoNoPuedeCompletarElMonitoreo() {
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        assertThatThrownBy(() -> servicio.completarMonitoreo(7L, 99L, sesion)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(asignaciones, bonita);
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
        todoFinalizado();
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
        prepararCierre(); when(asignaciones.existsByActividadEmergenciaIdAndFinalizadaFalse(7L)).thenReturn(true);
        assertThatThrownBy(() -> servicio.confirmar(7L, recibo("LIBERADA", 5))).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(ofertas, inventarios);
    }
    @Test void noSePuedeLiberarAntesDeLaValidacionDelCoordinador() {
        prepararCierre(); emergencia.setFechaValidacionDespliegue(null);
        assertThatThrownBy(() -> servicio.confirmar(7L, recibo("LIBERADA", 5))).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(ofertas, inventarios);
    }
}
