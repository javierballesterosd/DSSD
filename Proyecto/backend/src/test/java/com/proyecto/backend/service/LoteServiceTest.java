package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.ItemLoteRequest;
import com.proyecto.backend.dto.LoteRequest;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.BonitaIntegrationException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.mapper.ItemLoteMapper;
import com.proyecto.backend.mapper.LoteMapper;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.Lote;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.model.NivelGravedad;
import com.proyecto.backend.model.Recurso;
import com.proyecto.backend.model.Region;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.RecursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoteServiceTest {

    @Mock
    private LoteRepository loteRepository;
    @Mock
    private EmergenciaRepository emergenciaRepository;
    @Mock
    private RecursoRepository recursoRepository;
    @Mock
    private BonitaClient bonitaClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LoteService loteService;
    private Emergencia emergencia;
    private final BonitaSession session = new BonitaSession("session", "token");
    private static final Long REGION_ID = 1L;

    @BeforeEach
    void setUp() {
        ItemLoteMapper itemLoteMapper = new ItemLoteMapper();
        loteService = new LoteService(
                loteRepository, emergenciaRepository, recursoRepository,
                new LoteMapper(itemLoteMapper), itemLoteMapper, bonitaClient, eventPublisher);

        emergencia = new Emergencia();
        emergencia.setId(7L);
        emergencia.setBonitaCaseId("caso-7");
        emergencia.setNivelGravedad(NivelGravedad.ALTA);
        Region region = new Region();
        region.setId(REGION_ID);
        Municipio municipio = new Municipio();
        municipio.setNombre("La Plata");
        municipio.setBonitaGroupPath("/Municipio/Region1/LaPlata");
        municipio.setRegion(region);
        emergencia.setMunicipio(municipio);
    }

    private LoteRequest request(LocalDateTime apertura, LocalDateTime cierre) {
        return new LoteRequest("Lote", apertura, cierre, List.of(new ItemLoteRequest(1L, 10)));
    }

    private void emergenciaYRecursoExisten() {
        when(emergenciaRepository.findById(7L)).thenReturn(Optional.of(emergencia));
        when(loteRepository.findFirstByEmergenciaIdOrderByIdDesc(7L)).thenReturn(Optional.empty());
        Recurso recurso = new Recurso();
        recurso.setId(1L);
        when(recursoRepository.findById(1L)).thenReturn(Optional.of(recurso));
        when(loteRepository.save(any(Lote.class))).thenAnswer(invocation -> {
            Lote lote = invocation.getArgument(0);
            lote.setId(30L);
            return lote;
        });
    }

    @Test
    void rechazaUnCierreAnteriorALaApertura() {
        LocalDateTime ahora = LocalDateTime.now();

        assertThatThrownBy(() ->
                loteService.publicarLote(7L, request(ahora.plusDays(2), ahora.plusDays(1)), coordinadorDeRegion(REGION_ID), session)
        ).isInstanceOf(ReglaNegocioException.class);

        verify(loteRepository, never()).save(any());
    }

    @Test
    void rechazaPublicarSiYaHayUnLoteNoCancelado() {
        when(emergenciaRepository.findById(7L)).thenReturn(Optional.of(emergencia));
        Lote activo = new Lote();
        activo.setId(5L);
        activo.setEstado(EstadoLote.ACTIVO);
        when(loteRepository.findFirstByEmergenciaIdOrderByIdDesc(7L)).thenReturn(Optional.of(activo));
        LocalDateTime ahora = LocalDateTime.now();

        assertThatThrownBy(() ->
                loteService.publicarLote(7L, request(ahora, ahora.plusDays(1)), coordinadorDeRegion(REGION_ID), session)
        ).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void completaLaTareaDesglosarEnBonitaConElLoteYSuVentana() {
        emergenciaYRecursoExisten();
        when(bonitaClient.buscarTareaPendiente(session, "caso-7", LoteService.TAREA_DESGLOSAR_LOTES))
                .thenReturn("tarea-9");
        LocalDateTime apertura = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime cierre = apertura.plusDays(1).withHour(9).withMinute(30);

        loteService.publicarLote(7L, request(apertura, cierre), coordinadorDeRegion(REGION_ID), session);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> contrato = ArgumentCaptor.forClass(Map.class);
        verify(bonitaClient).ejecutarTarea(eq(session), eq("tarea-9"), contrato.capture());
        assertThat(contrato.getValue())
                .containsEntry("loteId", 30L)
                .containsEntry("fechaAperturaOfertas", apertura.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .containsEntry("fechaCierreOfertas", cierre.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Test
    void alPublicarAvisaAlMunicipioDeLaEmergencia() {
        emergenciaYRecursoExisten();
        when(bonitaClient.buscarTareaPendiente(any(), any(), any())).thenReturn("tarea-9");
        LocalDateTime ahora = LocalDateTime.now();
        LoginResponse coordinador = coordinadorDeRegion(REGION_ID);

        loteService.publicarLote(7L, request(ahora, ahora.plusDays(1)), coordinador, session);

        ArgumentCaptor<LoteCambiadoEvent> evento = ArgumentCaptor.forClass(LoteCambiadoEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().loteId()).isEqualTo(30L);
        assertThat(evento.getValue().estado()).isEqualTo(EstadoLote.ACTIVO);
        assertThat(evento.getValue().emergenciaId()).isEqualTo(7L);
        assertThat(evento.getValue().municipioGroupPath()).isEqualTo("/Municipio/Region1/LaPlata");
        assertThat(evento.getValue().remitente()).isSameAs(coordinador);
    }

    @Test
    void siFallaBonitaNoSeAvisaAlMunicipio() {
        emergenciaYRecursoExisten();
        when(bonitaClient.buscarTareaPendiente(any(), any(), any())).thenReturn("tarea-9");
        doThrow(new BonitaIntegrationException("falló")).when(bonitaClient).ejecutarTarea(any(), any(), any());
        LocalDateTime ahora = LocalDateTime.now();

        assertThatThrownBy(() -> loteService.publicarLote(
                7L, request(ahora, ahora.plusDays(1)), coordinadorDeRegion(REGION_ID), session)
        ).isInstanceOf(BonitaIntegrationException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rechazaUnLoteConElMismoRecursoRepetido() {
        LocalDateTime ahora = LocalDateTime.now();
        LoteRequest repetido = new LoteRequest("Lote", ahora, ahora.plusDays(1),
                List.of(new ItemLoteRequest(1L, 10), new ItemLoteRequest(1L, 5)));

        assertThatThrownBy(() ->
                loteService.publicarLote(7L, repetido, coordinadorDeRegion(REGION_ID), session)
        ).isInstanceOf(ReglaNegocioException.class);

        verify(loteRepository, never()).save(any());
    }

    @Test
    void rechazaUnaAperturaAnteriorALaFechaActual() {
        LocalDateTime ahora = LocalDateTime.now();

        assertThatThrownBy(() ->
                loteService.publicarLote(7L, request(ahora.minusHours(1), ahora.plusDays(1)), coordinadorDeRegion(REGION_ID), session)
        ).isInstanceOf(ReglaNegocioException.class);

        verify(loteRepository, never()).save(any());
    }

    @Test
    void rechazaPublicarParaUnaEmergenciaDeOtraRegion() {
        when(emergenciaRepository.findById(7L)).thenReturn(Optional.of(emergencia));
        LocalDateTime ahora = LocalDateTime.now();

        assertThatThrownBy(() ->
                loteService.publicarLote(7L, request(ahora, ahora.plusDays(1)), coordinadorDeRegion(2L), session)
        ).isInstanceOf(AccesoDenegadoException.class);

        verify(loteRepository, never()).save(any());
    }

    private Lote loteDeLaEmergencia() {
        return Lote.builder()
                .id(30L)
                .titulo("Lote")
                .estado(EstadoLote.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .emergencia(emergencia)
                .build();
    }

    private LoginResponse coordinadorDeRegion(Long regionId) {
        LoginResponse usuario = new LoginResponse();
        usuario.setRole("COORDINADOR");
        usuario.setRegionId(regionId);
        return usuario;
    }

    @Test
    void listaLosLotesDeLaRegionEnCualquierEstado() {
        when(loteRepository.findByEmergenciaMunicipioRegionIdOrderByFechaCreacionDesc(REGION_ID))
                .thenReturn(List.of(loteDeLaEmergencia()));

        var lotes = loteService.listarDeRegion(REGION_ID);

        assertThat(lotes).hasSize(1);
        assertThat(lotes.get(0).emergencia().municipio()).isEqualTo("La Plata");
    }

    @Test
    void elCoordinadorVeElDetalleDeUnLoteDeSuRegion() {
        when(loteRepository.findDetalleById(30L)).thenReturn(Optional.of(loteDeLaEmergencia()));

        var detalle = loteService.obtenerDetalle(30L, coordinadorDeRegion(REGION_ID));

        assertThat(detalle.id()).isEqualTo(30L);
    }

    @Test
    void elCoordinadorNoVeElDetalleDeUnLoteDeOtraRegion() {
        when(loteRepository.findDetalleById(30L)).thenReturn(Optional.of(loteDeLaEmergencia()));

        assertThatThrownBy(() -> loteService.obtenerDetalle(30L, coordinadorDeRegion(2L)))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void siFallaBonitaSePropagaElErrorParaQueSeRevierta()  {
        emergenciaYRecursoExisten();
        when(bonitaClient.buscarTareaPendiente(any(), any(), any())).thenReturn("tarea-9");
        BonitaIntegrationException error = new BonitaIntegrationException("falló");
        doThrow(error).when(bonitaClient).ejecutarTarea(any(), any(), any());
        LocalDateTime ahora = LocalDateTime.now();

        assertThatThrownBy(() ->
                loteService.publicarLote(7L, request(ahora, ahora.plusDays(1)), coordinadorDeRegion(REGION_ID), session)
        ).isSameAs(error);
    }

    @Test
    void rechazaUnaEmergenciaSinCasoEnBonita() {
        emergenciaYRecursoExisten();
        emergencia.setBonitaCaseId(null);
        LocalDateTime ahora = LocalDateTime.now();

        assertThatThrownBy(() ->
                loteService.publicarLote(7L, request(ahora, ahora.plusDays(1)), coordinadorDeRegion(REGION_ID), session)
        ).isInstanceOf(ReglaNegocioException.class);

        verify(bonitaClient, never()).ejecutarTarea(any(), any(), any());
    }
}
