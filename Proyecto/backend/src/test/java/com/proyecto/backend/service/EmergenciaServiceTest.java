package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.EmergenciaRequest;
import com.proyecto.backend.dto.DescriptorAudiencia;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.BonitaIntegrationException;
import com.proyecto.backend.mapper.EmergenciaMapper;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.model.NivelGravedad;
import com.proyecto.backend.model.Region;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.MunicipioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergenciaServiceTest {

    @Mock
    private EmergenciaRepository emergenciaRepository;
    @Mock
    private MunicipioRepository municipioRepository;
    @Mock
    private BonitaClient bonitaClient;
    @Mock
    private LoteRepository loteRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EmergenciaService emergenciaService;
    private LoginResponse remitente;
    private BonitaSession bonitaSession;
    private Municipio municipio;

    @BeforeEach
    void setUp() {
        emergenciaService = new EmergenciaService(
                emergenciaRepository,
                municipioRepository,
                bonitaClient,
                new EmergenciaMapper(),
                loteRepository,
                eventPublisher
        );

        Region region = new Region();
        region.setId(1L);
        region.setNombre("Región 1");
        region.setBonitaGroupPath("/Municipio/Region1");

        Municipio municipio = new Municipio();
        municipio.setId(5L);
        municipio.setNombre("La Plata");
        municipio.setBonitaGroupPath("/Municipio/Region1/LaPlata");
        municipio.setRegion(region);

        remitente = new LoginResponse();
        remitente.setUserId("municipal-1");
        remitente.setUsername("operador.laplata");
        remitente.setRole("MUNICIPAL");
        remitente.setGroupPath("/Municipio/Region1/LaPlata");

        bonitaSession = new BonitaSession("session", "token");

        this.municipio = municipio;

        lenient().when(municipioRepository.findById(5L)).thenReturn(Optional.of(municipio));
        lenient().when(bonitaClient.buscarProcesoId(bonitaSession)).thenReturn("proceso-1");
        lenient().when(emergenciaRepository.save(any(Emergencia.class))).thenAnswer(invocation -> {
            Emergencia emergencia = invocation.getArgument(0);
            if (emergencia.getId() == null) {
                emergencia.setId(20L);
            }
            return emergencia;
        });
    }

    private void bonitaResponde() {
        when(bonitaClient.iniciarCaso(bonitaSession, "proceso-1")).thenReturn("caso-1");
        when(bonitaClient.buscarTareaPendiente(
                bonitaSession, "caso-1", EmergenciaService.TAREA_REGISTRAR_EMERGENCIA
        )).thenReturn("tarea-1");
    }

    @Test
    void registraLaEmergenciaYCompletaLaTareaEnBonita() {
        bonitaResponde();

        var respuesta = emergenciaService.registrarEmergencia(request(), 5L, remitente, bonitaSession);

        verify(bonitaClient).ejecutarTarea(eq(bonitaSession), eq("tarea-1"), any(Map.class));
        assertThat(respuesta).isNotNull();
        verify(bonitaClient, never()).cancelarCaso(any(), any());
    }

    @Test
    void publicaElEventoParaNotificarALosCoordinadoresDeLaRegion() {
        bonitaResponde();

        emergenciaService.registrarEmergencia(request(), 5L, remitente, bonitaSession);

        ArgumentCaptor<EmergenciaRegistradaEvent> captor =
                ArgumentCaptor.forClass(EmergenciaRegistradaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        EmergenciaRegistradaEvent evento = captor.getValue();
        assertThat(evento.regionGroupPath()).isEqualTo("/Municipio/Region1");
        assertThat(evento.municipioNombre()).isEqualTo("La Plata");
        assertThat(evento.remitente()).isSameAs(remitente);
    }

    @Test
    void siFallaLaTareaDeBonitaSeEliminaElCasoYSePropagaElError() {
        bonitaResponde();
        BonitaIntegrationException error = new BonitaIntegrationException("falló la tarea");
        doThrow(error).when(bonitaClient).ejecutarTarea(any(), any(), any());

        assertThatThrownBy(() ->
                emergenciaService.registrarEmergencia(request(), 5L, remitente, bonitaSession)
        ).isSameAs(error);

        verify(bonitaClient).cancelarCaso(bonitaSession, "caso-1");
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void siFallaAntesDeCrearElCasoNoHayNadaQueEliminar() {
        BonitaIntegrationException error = new BonitaIntegrationException("sin conexión");
        when(bonitaClient.iniciarCaso(bonitaSession, "proceso-1")).thenThrow(error);

        assertThatThrownBy(() ->
                emergenciaService.registrarEmergencia(request(), 5L, remitente, bonitaSession)
        ).isSameAs(error);

        verify(bonitaClient, never()).cancelarCaso(any(), any());
    }

    @Test
    void siFallaLaCompensacionSePropagaElErrorOriginal() {
        bonitaResponde();
        BonitaIntegrationException error = new BonitaIntegrationException("falló la tarea");
        doThrow(error).when(bonitaClient).ejecutarTarea(any(), any(), any());
        doThrow(new BonitaIntegrationException("no se pudo eliminar"))
                .when(bonitaClient).cancelarCaso(any(), any());

        assertThatThrownBy(() ->
                emergenciaService.registrarEmergencia(request(), 5L, remitente, bonitaSession)
        ).isSameAs(error);
    }

    private EmergenciaRequest request() {
        return new EmergenciaRequest(
                NivelGravedad.ALTA, "Zona Norte", "Desborde del arroyo con familias evacuadas");
    }


    private Emergencia emergenciaGuardada() {
        Emergencia emergencia = new Emergencia();
        emergencia.setId(20L);
        emergencia.setNivelGravedad(NivelGravedad.CRITICA);
        emergencia.setZonaAfectada("Barrio Norte");
        emergencia.setDescripcion("Inundación en el casco urbano");
        emergencia.setFechaRegistro(LocalDateTime.now());
        emergencia.setMunicipio(municipio);
        return emergencia;
    }

    @Test
    void listaLasEmergenciasDelMunicipioConElEstadoDeSuLote() {
        when(emergenciaRepository.findByMunicipioIdOrderByFechaRegistroDesc(5L))
                .thenReturn(List.of(emergenciaGuardada()));
        when(loteRepository.findFirstByEmergenciaIdOrderByIdDesc(20L)).thenReturn(Optional.empty());

        var emergencias = emergenciaService.listarDeMunicipio(5L);

        assertThat(emergencias).hasSize(1);
        assertThat(emergencias.get(0).municipio()).isEqualTo("La Plata");
        assertThat(emergencias.get(0).nivelGravedadEtiqueta()).isEqualTo("Crítica");
        assertThat(emergencias.get(0).loteId()).isNull();
    }

    @Test
    void elOperadorVeElDetalleDeUnaEmergenciaDeSuMunicipio() {
        when(emergenciaRepository.findById(20L)).thenReturn(Optional.of(emergenciaGuardada()));
        when(loteRepository.findFirstByEmergenciaIdOrderByIdDesc(20L)).thenReturn(Optional.empty());
        remitente.setMunicipioId(5L);

        assertThat(emergenciaService.obtenerDetalle(20L, remitente).id()).isEqualTo(20L);
    }

    @Test
    void elOperadorNoVeElDetalleDeUnaEmergenciaDeOtroMunicipio() {
        when(emergenciaRepository.findById(20L)).thenReturn(Optional.of(emergenciaGuardada()));
        remitente.setMunicipioId(9L);

        assertThatThrownBy(() -> emergenciaService.obtenerDetalle(20L, remitente))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void elCoordinadorNoVeElDetalleDeUnaEmergenciaDeOtraRegion() {
        when(emergenciaRepository.findById(20L)).thenReturn(Optional.of(emergenciaGuardada()));
        LoginResponse coordinador = new LoginResponse();
        coordinador.setRole("COORDINADOR");
        coordinador.setRegionId(2L);

        assertThatThrownBy(() -> emergenciaService.obtenerDetalle(20L, coordinador))
                .isInstanceOf(AccesoDenegadoException.class);
    }
}
