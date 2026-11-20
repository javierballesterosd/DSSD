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
        service = new DecisionMunicipalService(emergencias, lotes, ofertas, inventarios,
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
        emergencia.setZonaAfectada("Plaza Moreno");
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
        item.setCantidadRequerida(100);
        var ong = new Ong();
        ong.setId(2L);
        ong.setCodigo("cod-2");
        ong.setBonitaGroupPath("/ONG/Caritas");
        ong.setRazonSocial("Cáritas");
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
        usuario.setUserId("42");
    }

    void prepararDecision() {
        when(transactions.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(lotes.bloquearPorId(10L)).thenReturn(Optional.of(lote));
        lenient().when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta));
        lenient().when(bonita.buscarTareaPendiente(session, "caso-7", "Ver ofertas validadas")).thenReturn("tarea");
    }

    IntegracionNacional.Respuesta resultado(boolean aceptar, int cantidad) {
        return new IntegracionNacional.Respuesta(10L, "caso-7", List.of(new IntegracionNacional.Resultado(20L, true,
                List.of(new IntegracionNacional.ItemSN("cod-2", 3L, cantidad, aceptar ? "COMPROMETIDA" : "LIBERADA")))));
    }

    @Test void sumaLasCantidadesAceptadasYDesglosaCuantoAportaCadaOng() {
        when(emergencias.findById(7L)).thenReturn(Optional.of(emergencia));
        when(lotes.findFirstByEmergenciaIdOrderByIdDesc(7L)).thenReturn(Optional.of(lote));
        var segunda = new Oferta();
        segunda.setEstado(EstadoOferta.VALIDADA);
        var otraOng = new Ong();
        otraOng.setId(4L);
        otraOng.setRazonSocial("Bomberos");
        var d = new DetalleOferta();
        d.setOng(otraOng);
        d.setItemLote(oferta.getDetalles().getFirst().getItemLote());
        d.setCantidadAceptada(10);
        segunda.getDetalles().add(d);
        when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta, segunda));
        var respuesta = service.consultar(7L, 1L);
        assertThat(respuesta.recursos()).hasSize(1);
        assertThat(respuesta.recursos().getFirst().cantidadRequerida()).isEqualTo(100);
        assertThat(respuesta.recursos().getFirst().cantidad()).isEqualTo(50);
        assertThat(respuesta.recursos().getFirst().porOng()).extracting(
                RecursosMunicipalesResponse.AporteOng::razonSocial, RecursosMunicipalesResponse.AporteOng::cantidad)
                .containsExactly(tuple("Cáritas", 40L), tuple("Bomberos", 10L));
        assertThat(respuesta.puedeDecidir()).isTrue();
    }

    @Test void aceptarBloqueaElStockSoloUnaVezAlConfirmarDesdeBonita() {
        prepararDecision();

        var inventario = new InventarioOng(); inventario.setCantidadDisponible(200);
        when(inventarios.findByOngIdAndRecursoIdForUpdate(2L, 3L)).thenReturn(Optional.of(inventario));
        service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session);
        assertThat(inventario.getCantidadDisponible()).isEqualTo(200);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ACEPTADA_PARCIAL);
        assertThat(lote.getFechaDecisionMunicipal()).isNull();
        service.confirmar(7L, resultado(true, 40));
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ADJUDICADA);

        assertThat(inventario.getCantidadDisponible()).isEqualTo(160);
        service.confirmar(7L, resultado(true, 40));
        assertThat(inventario.getCantidadDisponible()).isEqualTo(160);
        verify(inventarios, times(1)).save(inventario);
        verify(bonita).ejecutarTareaIdempotente(session, "tarea", Map.of("aceptaOfertas", true));
        assertThat(lote.getFechaDecisionMunicipal()).isNotNull();
    }

    @Test void noConfirmaSiElStockLocalEsInsuficiente() {
        prepararDecision();
        var inventario = new InventarioOng(); inventario.setCantidadDisponible(39);
        when(inventarios.findByOngIdAndRecursoIdForUpdate(2L, 3L)).thenReturn(Optional.of(inventario));
        service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session);
        assertThatThrownBy(() -> service.confirmar(7L, resultado(true, 40)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("stock local suficiente");
        assertThat(inventario.getCantidadDisponible()).isEqualTo(39);
        assertThat(lote.getFechaDecisionMunicipal()).isNull();
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ACEPTADA_PARCIAL);
        verify(inventarios, never()).save(any());
        verifyNoInteractions(eventos);
        inventario.setCantidadDisponible(50);
        service.confirmar(7L, resultado(true, 40));
        assertThat(inventario.getCantidadDisponible()).isEqualTo(10);
    }

    @Test void agrupaElMismoRecursoDeVariasOfertasAntesDeDescontar() {
        prepararDecision();
        var segunda = new Oferta(); segunda.setId(21L); segunda.setEstado(EstadoOferta.VALIDADA);
        segunda.getDetalles().addAll(oferta.getDetalles());
        when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta, segunda));
        var inventario = new InventarioOng(); inventario.setCantidadDisponible(60);
        when(inventarios.findByOngIdAndRecursoIdForUpdate(2L, 3L)).thenReturn(Optional.of(inventario));
        service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session);
        var primero = resultado(true, 40).resultados().getFirst();
        var segundo = new IntegracionNacional.Resultado(21L, true, primero.items());
        var respuesta = new IntegracionNacional.Respuesta(10L, "caso-7", List.of(primero, segundo));
        assertThatThrownBy(() -> service.confirmar(7L, respuesta)).isInstanceOf(ReglaNegocioException.class);
        assertThat(inventario.getCantidadDisponible()).isEqualTo(60);
        verify(inventarios, never()).save(any());
        inventario.setCantidadDisponible(100);
        service.confirmar(7L, respuesta);
        assertThat(inventario.getCantidadDisponible()).isEqualTo(20);
        verify(inventarios, times(1)).save(inventario);
    }

    @Test void rechazarLiberaEnSnSinModificarStockLocalYNotificaTodasLasOngs() {
        prepararDecision();
        var rechazada = new Oferta();
        rechazada.setEstado(EstadoOferta.RECHAZADA);
        var otraOng = new Ong(); otraOng.setBonitaGroupPath("/ONG/Otra");
        rechazada.getOngs().add(otraOng);
        when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta, rechazada));
        var inventario = new InventarioOng(); inventario.setCantidadDisponible(160);
        service.decidir(7L, new DecisionMunicipalRequest(10L, false), usuario, session);
        assertThat(inventario.getCantidadDisponible()).isEqualTo(160);
        service.confirmar(7L, resultado(false, 40));
        assertThat(inventario.getCantidadDisponible()).isEqualTo(160);
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.NO_ADJUDICADA);
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(emergencia.isRequiereActualizacion()).isTrue();
        verify(eventos).publishEvent(argThat((Object e) -> e instanceof DecisionMunicipalEvent decision
                && decision.ongGroupPaths().equals(Set.of("/ONG/Caritas", "/ONG/Otra"))));
        // Un segundo envío no devuelve stock ni notifica de nuevo.
        service.decidir(7L, new DecisionMunicipalRequest(10L, false), usuario, session);
        verifyNoInteractions(inventarios);
        service.confirmar(7L, resultado(false, 40));
        verifyNoInteractions(inventarios);
        verify(eventos, times(1)).publishEvent(any(Object.class));
    }

    @Test void fallaBonitaConservaIntencionYNoModificaStockLocal() {
        prepararDecision();
        doThrow(new BonitaIntegrationException("Sin conexión"))
                .when(bonita).ejecutarTareaIdempotente(any(), any(), any());
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, false), usuario, session))
                .isInstanceOf(BonitaIntegrationException.class);
        assertThat(lote.getDecisionMunicipal()).isFalse();
        assertThat(lote.getFechaDecisionMunicipal()).isNull();
        assertThat(lote.getTareaDecisionBonitaId()).isEqualTo("tarea");
        assertThat(oferta.getEstado()).isEqualTo(EstadoOferta.ACEPTADA_PARCIAL);
        verifyNoInteractions(inventarios, eventos);
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test void noPermiteDecidirSiQuedanOfertasPendientes() {
        prepararDecision();
        oferta.setEstado(EstadoOferta.PENDIENTE);
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("validar");
        verifyNoInteractions(inventarios);
    }

    @Test void noConfirmaSiLasReservasNacionalesNoCoincidenConLasCantidadesLocales() {
        prepararDecision();
        service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session);
        assertThatThrownBy(() -> service.confirmar(7L, resultado(true, 100)))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no coinciden");
        verifyNoInteractions(inventarios, eventos);
        assertThat(lote.getFechaDecisionMunicipal()).isNull();
    }

    @Test void rechazaCallbackDeOtroCasoYNoAdjudicaSinResultado() {
        prepararDecision();
        service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session);
        assertThatThrownBy(() -> service.confirmar(7L,
                new IntegracionNacional.Respuesta(10L, "otro", List.of())))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> service.confirmar(7L,
                new IntegracionNacional.Respuesta(10L, "caso-7", List.of())))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("Falta confirmar");
        verifyNoInteractions(inventarios, eventos);
    }

    @Test void noLiberaLocalmenteSiElResultadoTodaviaTieneRecursosBloqueados() {
        prepararDecision();
        service.decidir(7L, new DecisionMunicipalRequest(10L, false), usuario, session);
        assertThatThrownBy(() -> service.confirmar(7L, new IntegracionNacional.Respuesta(10L, "caso-7",
                List.of(new IntegracionNacional.Resultado(20L, true,
                        List.of(new IntegracionNacional.ItemSN("cod-2", 3L, 40, "BLOQUEADA")))))))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(inventarios, eventos);
    }

    @Test void noPermiteDecidirAntesDelCierre() {
        prepararDecision();
        lote.setFechaCierreOfertas(LocalDateTime.now().plusDays(1));
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("cerrar");
        verifyNoInteractions(inventarios);
    }

    @Test void permiteRechazarAunqueElSnNoHayaHabilitadoRecursos() {
        prepararDecision();
        oferta.setEstado(EstadoOferta.RECHAZADA);
        service.decidir(7L, new DecisionMunicipalRequest(10L, false), usuario, session);
        service.confirmar(7L, new IntegracionNacional.Respuesta(10L, "caso-7", List.of()));
        assertThat(emergencia.isRequiereActualizacion()).isTrue();
        verifyNoInteractions(inventarios);
    }

    @Test void noPermiteAceptarSiElSnRechazoTodosLosRecursos() {
        prepararDecision();
        oferta.setEstado(EstadoOferta.RECHAZADA);
        assertThatThrownBy(() -> service.decidir(7L, new DecisionMunicipalRequest(10L, true), usuario, session))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no habilitó recursos");
        verifyNoInteractions(inventarios);
    }

    @Test void impideAccesoDeOtroMunicipio() {
        when(emergencias.findById(7L)).thenReturn(Optional.of(emergencia));
        assertThatThrownBy(() -> service.consultar(7L, 99L)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(lotes);
    }

    @Test void actualizaLaMismaEmergenciaSinCrearCasoNiCambiarSuFechaOriginal() {
        emergencia.setRequiereActualizacion(true);
        emergencia.setFechaRegistro(LocalDateTime.now().minusDays(2));
        LocalDateTime fechaOriginal = emergencia.getFechaRegistro();
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(bonita.buscarTareaPendiente(session, "caso-7", "Registrar emergencia")).thenReturn("registrar");
        var respuesta = service.actualizar(7L, new EmergenciaRequest(NivelGravedad.ALTA, "Zona", "Descripción actualizada", List.of()), usuario, session);
        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(emergencia.getBonitaCaseId()).isEqualTo("caso-7");
        assertThat(emergencia.getFechaRegistro()).isEqualTo(fechaOriginal);
        assertThat(emergencia.isRequiereActualizacion()).isFalse();
        verify(bonita, never()).iniciarCaso(any(), any());
    }

    @Test void alActualizarSeEditanLasActividadesExistentesYSeAgreganLasNuevas() {
        emergencia.setRequiereActualizacion(true);
        var existente = new Actividad();
        existente.setNombre("Evacuación");
        existente.setFinalizada(false);
        emergencia.setActividades(new ArrayList<>(List.of(existente)));
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(bonita.buscarTareaPendiente(session, "caso-7", "Registrar emergencia")).thenReturn("registrar");

        service.actualizar(7L, new EmergenciaRequest(NivelGravedad.ALTA, "Zona", "Descripción actualizada",
                List.of(new ActividadRequest("Evacuación", "Con botes"), new ActividadRequest("Reparto de agua", null))),
                usuario, session);

        assertThat(emergencia.getActividades()).hasSize(2);
        assertThat(emergencia.getActividades().get(0)).isSameAs(existente);
        assertThat(existente.getDescripcion()).isEqualTo("Con botes");
        assertThat(emergencia.getActividades().get(1).getNombre()).isEqualTo("Reparto de agua");
        assertThat(emergencia.getActividades().get(1).getEmergencia()).isSameAs(emergencia);
        assertThat(emergencia.getActividades().get(1).isFinalizada()).isFalse();
    }

    @Test void alActualizarSeQuitanLasActividadesQueSobran() {
        emergencia.setRequiereActualizacion(true);
        var a = new Actividad(); a.setNombre("Una");
        var b = new Actividad(); b.setNombre("Dos");
        emergencia.setActividades(new ArrayList<>(List.of(a, b)));
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(bonita.buscarTareaPendiente(session, "caso-7", "Registrar emergencia")).thenReturn("registrar");

        service.actualizar(7L, new EmergenciaRequest(NivelGravedad.ALTA, "Zona", "Descripción actualizada",
                List.of(new ActividadRequest("Una", null))), usuario, session);

        assertThat(emergencia.getActividades()).containsExactly(a);
    }

    // ---------- El SN no habilitó recursos en ninguna oferta ----------

    void prepararSinRecursos(EstadoOferta estadoOferta) {
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(lotes.bloquearPorId(10L)).thenReturn(Optional.of(lote));
        oferta.setEstado(estadoOferta);
        lenient().when(ofertas.findByLoteIdOrderByIdAsc(10L)).thenReturn(List.of(oferta));
    }

    @Test void sinRecursosCancelaElLoteMarcaLaEmergenciaYAvisa() {
        prepararSinRecursos(EstadoOferta.RECHAZADA);

        service.confirmarSinRecursos(7L, new IntegracionNacional.SinRecursos(10L, "caso-7"));

        assertThat(lote.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(lote.getFechaDecisionMunicipal()).isNotNull();
        assertThat(lote.getDecisionMunicipal()).isNull();
        assertThat(emergencia.isRequiereActualizacion()).isTrue();
        var evento = org.mockito.ArgumentCaptor.forClass(SinRecursosEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().emergenciaId()).isEqualTo(7L);
        assertThat(evento.getValue().emergenciaTitulo()).isEqualTo("Plaza Moreno");
        assertThat(evento.getValue().municipioGroupPath()).isEqualTo("/Municipio/Region1/LaPlata");
        assertThat(evento.getValue().ongGroupPaths()).containsExactly("/ONG/Caritas");
    }

    @Test void sinRecursosNoAvisaALasOngsDeOfertasEliminadas() {
        prepararSinRecursos(EstadoOferta.ELIMINADA);

        service.confirmarSinRecursos(7L, new IntegracionNacional.SinRecursos(10L, "caso-7"));

        var evento = org.mockito.ArgumentCaptor.forClass(SinRecursosEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().ongGroupPaths()).isEmpty();
    }

    @Test void sinRecursosEsIdempotente() {
        prepararSinRecursos(EstadoOferta.RECHAZADA);
        var pedido = new IntegracionNacional.SinRecursos(10L, "caso-7");

        service.confirmarSinRecursos(7L, pedido);
        service.confirmarSinRecursos(7L, pedido);

        verify(eventos, times(1)).publishEvent(any(SinRecursosEvent.class));
        verify(lotes, times(1)).saveAndFlush(lote);
    }

    @Test void sinRecursosRechazaSiHayOfertasHabilitadasOPendientes() {
        for (var estado : List.of(EstadoOferta.VALIDADA, EstadoOferta.ACEPTADA_PARCIAL, EstadoOferta.PENDIENTE)) {
            lote.setFechaDecisionMunicipal(null);
            lote.setEstado(EstadoLote.ACTIVO);
            prepararSinRecursos(estado);
            assertThatThrownBy(() -> service.confirmarSinRecursos(7L, new IntegracionNacional.SinRecursos(10L, "caso-7")))
                    .isInstanceOf(ReglaNegocioException.class);
        }
        verify(eventos, never()).publishEvent(any());
        assertThat(lote.getEstado()).isEqualTo(EstadoLote.ACTIVO);
    }

    @Test void sinRecursosValidaElCasoDeBonitaYLaVentanaDeOfertas() {
        when(emergencias.bloquearPorId(7L)).thenReturn(Optional.of(emergencia));
        when(lotes.bloquearPorId(10L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> service.confirmarSinRecursos(7L, new IntegracionNacional.SinRecursos(10L, "otro-caso")))
                .isInstanceOf(ReglaNegocioException.class);

        lote.setFechaCierreOfertas(LocalDateTime.now().plusHours(1));
        assertThatThrownBy(() -> service.confirmarSinRecursos(7L, new IntegracionNacional.SinRecursos(10L, "caso-7")))
                .isInstanceOf(ReglaNegocioException.class);
        verify(eventos, never()).publishEvent(any());
    }

    @Test void despuesDeCerrarSinRecursosElMunicipioVeElMotivo() {
        prepararSinRecursos(EstadoOferta.RECHAZADA);
        service.confirmarSinRecursos(7L, new IntegracionNacional.SinRecursos(10L, "caso-7"));
        when(emergencias.findById(7L)).thenReturn(Optional.of(emergencia));
        when(lotes.findFirstByEmergenciaIdOrderByIdDesc(7L)).thenReturn(Optional.of(lote));

        var respuesta = service.consultar(7L, 1L);

        assertThat(respuesta.confirmada()).isTrue();
        assertThat(respuesta.puedeDecidir()).isFalse();
        assertThat(respuesta.mensaje()).contains("no habilitó recursos").contains("volver a registrar");
    }
}
