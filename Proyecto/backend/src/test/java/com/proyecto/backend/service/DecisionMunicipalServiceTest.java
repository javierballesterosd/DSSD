package com.proyecto.backend.service;

import com.proyecto.backend.client.*;
import com.proyecto.backend.dto.*;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.exception.*;
import com.proyecto.backend.mapper.EmergenciaMapper;
import com.proyecto.backend.model.*;
import com.proyecto.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionMunicipalServiceTest {
    @Mock EmergenciaRepository emergencias;
    @Mock LoteRepository lotes;
    @Mock OfertaRepository ofertas;
    @Mock InventarioOngRepository inventarios;
    @Mock SistemaNacionalClient nacional;
    @Mock BonitaClient bonita;
    @Mock ApplicationEventPublisher eventos;
    @Mock PlatformTransactionManager transactions;
    DecisionMunicipalService service;
    Emergencia emergencia;
    Lote lote;
    Oferta oferta;
    LoginResponse usuario;
    final BonitaSession session = new BonitaSession("session", "token");

    @BeforeEach
    void preparar() {
        service = new DecisionMunicipalService(emergencias, lotes, ofertas, inventarios, nacional,
                bonita, new EmergenciaMapper(), eventos, transactions);
        var municipio = new Municipio();
        municipio.setId(1L);
        municipio.setBonitaGroupPath("/Municipio/Region1/LaPlata");
        municipio.setNombre("La Plata");
        var region = new Region();
        region.setBonitaGroupPath("/Municipio/Region1");
        municipio.setRegion(region);
        emergencia = new Emergencia();
        emergencia.setId(7L);
        emergencia.setMunicipio(municipio);
        emergencia.setNivelGravedad(NivelGravedad.ALTA);
        emergencia.setBonitaCaseId("caso-7");
        lote = new Lote();
        lote.setId(10L);
        lote.setEmergencia(emergencia);
        lote.setEstado(EstadoLote.ACTIVO);
        lote.setFechaCierreOfertas(LocalDateTime.now().minusHours(1));
        var recurso = new Recurso();
        recurso.setId(3L);
        recurso.setNombre("Frazadas");
        recurso.setUnidadMedida("unidades");
        var item = new ItemLote();
        item.setRecurso(recurso);
        var ong = new Ong();
        ong.setId(2L);
        ong.setBonitaGroupPath("/ONG/Caritas");
        oferta = new Oferta();
        oferta.setId(20L);
        oferta.setLote(lote);
        oferta.setEstado(EstadoOferta.ACEPTADA_PARCIAL);
        oferta.getOngs().add(ong);
        var detalle = new DetalleOferta();
        detalle.setCantidadOfrecida(100);
        detalle.setCantidadAceptada(40);
        detalle.setOng(ong);
        detalle.setItemLote(item);
        oferta.getDetalles().add(detalle);
        usuario = new LoginResponse();
        usuario.setMunicipioId(1L);
        usuario.setUsername("operador.laplata");
    }

    void prepararDecision() {
        when(transactions.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(lotes.bloquearPorId(10L)).thenReturn(Optional.of(lote));
        lenient().when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta));
        lenient().when(nacional.autenticar()).thenReturn("jwt");
        lenient().when(bonita.buscarTareaPendiente(session, "caso-7", "Ver ofertas validadas")).thenReturn("tarea");
    }

    @Test void sumaSoloCantidadesAceptadasSinDesgloseDeOngs() {
        when(emergencias.findById(7L)).thenReturn(Optional.of(emergencia));
        when(lotes.findFirstByEmergenciaIdOrderByIdDesc(7L)).thenReturn(Optional.of(lote));
        var segunda = new Oferta();
        segunda.setEstado(EstadoOferta.VALIDADA);
        var d = new DetalleOferta();
        d.setItemLote(oferta.getDetalles().getFirst().getItemLote());
        d.setCantidadAceptada(10);
        segunda.getDetalles().add(d);
        when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta, segunda));
        var respuesta = service.consultar(7L, 1L);
        assertThat(respuesta.recursos()).hasSize(1);
        assertThat(respuesta.recursos().getFirst().cantidad()).isEqualTo(50);
        assertThat(respuesta.puedeDecidir()).isTrue();
    }

    @Test void aceptarComprometeSinDescontarElStockOtraVez() {
        prepararDecision();
        when(nacional.consultar(anyString(), any())).thenReturn(new SistemaNacionalClient.Reservas(
                List.of(new SistemaNacionalClient.ItemReserva(2L, 3L, 40, "BLOQUEADA"))));
        service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ADJUDICADA);
        verify(nacional).decidir("jwt", new SistemaNacionalClient.Reserva(7L, 10L, 20L), true);
        verifyNoInteractions(inventarios);
        verify(bonita).ejecutarTarea(session, "tarea", Map.of("aceptaOfertas", true));
        assertThat(lote.getFechaDecisionMunicipal()).isNotNull();
    }

    @Test void rechazarLiberaSoloLoAceptadoYNotificaTodasLasOngs() {
        prepararDecision();
        var rechazada = new Oferta();
        rechazada.setEstado(EstadoOferta.RECHAZADA);
        var otraOng = new Ong(); otraOng.setBonitaGroupPath("/ONG/Otra");
        rechazada.getOngs().add(otraOng);
        when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta, rechazada));
        var inventario = new InventarioOng(); inventario.setCantidadDisponible(160);
        when(inventarios.findByOngIdAndRecursoIdForUpdate(2L, 3L)).thenReturn(Optional.of(inventario));
        service.decidir(7L, new DecisionMunicipalRequest(10L, false), usuario, session);
        assertThat(inventario.getCantidadDisponible()).isEqualTo(200);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.NO_ADJUDICADA);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(emergencia.isRequiereActualizacion()).isTrue();
        verify(eventos).publishEvent(argThat((Object e) -> e instanceof DecisionMunicipalEvent decision
                && decision.ongGroupPaths().equals(Set.of("/ONG/Caritas", "/ONG/Otra"))));
        // Un segundo envío no devuelve stock ni notifica de nuevo.
        service.decidir(7L, new DecisionMunicipalRequest(10L, false), usuario, session);
        verify(inventarios, times(1)).save(any());
        verify(nacional, times(1)).decidir(anyString(), any(), eq(false));
    }

    @Test void fallaNacionalConservaIntencionYNoModificaStockLocal() {
        prepararDecision();
        doThrow(new SistemaNacionalException("Sin conexión", new RuntimeException()))
                .when(nacional).decidir(anyString(), any(), eq(false));
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, false), usuario, session))
                .isInstanceOf(SistemaNacionalException.class);
        assertThat(lote.getDecisionMunicipal()).isFalse();
        assertThat(lote.getFechaDecisionMunicipal()).isNull();
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ACEPTADA_PARCIAL);
        verifyNoInteractions(inventarios, bonita, eventos);
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test void noPermiteDecidirSiQuedanOfertasPendientes() {
        prepararDecision();
        oferta.setEstado(EstadoOferta.PENDIENTE);
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("validar");
        verifyNoInteractions(nacional);
    }

    @Test void noComprometeSiLasReservasNacionalesNoCoincidenConLasCantidadesLocales() {
        prepararDecision();
        when(nacional.consultar(anyString(), any())).thenReturn(new SistemaNacionalClient.Reservas(
                List.of(new SistemaNacionalClient.ItemReserva(2L, 3L, 100, "BLOQUEADA"))));
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no coinciden");
        verify(nacional, never()).decidir(anyString(), any(), anyBoolean());
        verifyNoInteractions(inventarios, bonita);
    }

    @Test void noPermiteDecidirAntesDelCierre() {
        prepararDecision();
        lote.setFechaCierreOfertas(LocalDateTime.now().plusDays(1));
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("cerrar");
        verifyNoInteractions(nacional);
    }

    @Test void permiteRechazarAunqueElSnNoHayaHabilitadoRecursos() {
        prepararDecision();
        oferta.setEstado(EstadoOferta.RECHAZADA);
        service.decidir(7L, new DecisionMunicipalRequest(10L, false), usuario, session);
        assertThat(emergencia.isRequiereActualizacion()).isTrue();
        verifyNoInteractions(nacional, inventarios);
    }

    @Test void noPermiteAceptarSiElSnRechazoTodosLosRecursos() {
        prepararDecision();
        oferta.setEstado(EstadoOferta.RECHAZADA);
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no habilitó recursos");
        verifyNoInteractions(nacional, inventarios);
    }

    @Test void impideAccesoDeOtroMunicipio() {
        when(emergencias.findById(7L)).thenReturn(Optional.of(emergencia));
        assertThatThrownBy(() -> service.consultar(7L, 99L)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(lotes, nacional);
    }

    @Test void actualizaLaMismaEmergenciaSinCrearCasoNiCambiarSuFechaOriginal() {
        emergencia.setRequiereActualizacion(true);
        emergencia.setFechaRegistro(LocalDateTime.now().minusDays(2));
        LocalDateTime fechaOriginal = emergencia.getFechaRegistro();
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(bonita.buscarTareaPendiente(session, "caso-7", "Registrar emergencia")).thenReturn("registrar");
        var respuesta = service.actualizar(7L, new EmergenciaRequest(NivelGravedad.ALTA, "Zona", "Descripción actualizada"), usuario, session);
        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(emergencia.getBonitaCaseId()).isEqualTo("caso-7");
        assertThat(emergencia.getFechaRegistro()).isEqualTo(fechaOriginal);
        assertThat(emergencia.isRequiereActualizacion()).isFalse();
        verify(bonita, never()).iniciarCaso(any(), any());
    }
}
