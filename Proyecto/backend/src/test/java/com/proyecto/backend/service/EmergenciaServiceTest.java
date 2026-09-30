package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.emergencia.EmergenciaRequestDTO;
import com.proyecto.backend.dto.notificacion.DescriptorAudiencia;
import com.proyecto.backend.dto.notificacion.NotificacionResponseDTO;
import com.proyecto.backend.mapper.EmergenciaMapper;
import com.proyecto.backend.model.Emergencia;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.model.NivelGravedad;
import com.proyecto.backend.model.Region;
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.MunicipioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
    private NotificacionService notificacionService;

    private EmergenciaService emergenciaService;
    private Municipio municipio;
    private LoginResponse remitente;
    private BonitaSession bonitaSession;

    @BeforeEach
    void setUp() {
        emergenciaService = new EmergenciaService(
                emergenciaRepository,
                municipioRepository,
                bonitaClient,
                new EmergenciaMapper(),
                notificacionService
        );

        Region region = new Region();
        region.setId(1L);
        region.setNombre("Región 1");
        region.setBonitaGroupPath("/Municipio/Region1");

        municipio = new Municipio();
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

        when(municipioRepository.findById(5L)).thenReturn(Optional.of(municipio));
        when(bonitaClient.buscarProcesoId(bonitaSession, "Sistema")).thenReturn("proceso-1");
        when(bonitaClient.iniciarCaso(bonitaSession, "proceso-1")).thenReturn("caso-1");
        when(bonitaClient.buscarTareaPendiente(
                bonitaSession, "caso-1", EmergenciaService.TAREA_REGISTRAR_EMERGENCIA
        )).thenReturn("tarea-1");
        when(emergenciaRepository.save(any(Emergencia.class))).thenAnswer(invocation -> {
            Emergencia emergencia = invocation.getArgument(0);
            if (emergencia.getId() == null) {
                emergencia.setId(20L);
            }
            return emergencia;
        });
    }

    @Test
    void creaNotificacionParaCoordinadoresDeLaRegion() {
        EmergenciaRequestDTO request = request();

        emergenciaService.registrarEmergencia(request, 5L, bonitaSession, remitente);

        ArgumentCaptor<String> rolCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DescriptorAudiencia> audienciaCaptor =
                ArgumentCaptor.forClass(DescriptorAudiencia.class);
        verify(notificacionService).crear(
                rolCaptor.capture(),
                audienciaCaptor.capture(),
                eq("Nueva emergencia registrada"),
                any(String.class),
                eq(remitente)
        );

        assertThat(rolCaptor.getValue()).isEqualTo("COORDINADOR");
        assertThat(audienciaCaptor.getValue().grupoDestinatario())
                .isEqualTo("/Municipio/Region1");
        verify(bonitaClient).ejecutarTarea(eq(bonitaSession), eq("tarea-1"), any(Map.class));
    }

    @Test
    void laNotificacionUsaElRemitenteMunicipalSinCambiarlo() {
        emergenciaService.registrarEmergencia(request(), 5L, bonitaSession, remitente);

        verify(notificacionService).crear(
                any(String.class),
                any(DescriptorAudiencia.class),
                any(String.class),
                any(String.class),
                eq(remitente)
        );
    }

    @Test
    void siFallaLaNotificacionLaOperacionPropagaElErrorYNoGuardaLaEmergenciaFinal() {
        RuntimeException error = new RuntimeException("fallo de persistencia");
        when(notificacionService.crear(
                any(String.class),
                any(DescriptorAudiencia.class),
                any(String.class),
                any(String.class),
                eq(remitente)
        )).thenThrow(error);

        assertThatThrownBy(() ->
                emergenciaService.registrarEmergencia(request(), 5L, bonitaSession, remitente)
        ).isSameAs(error);

        verify(emergenciaRepository, times(1)).save(any(Emergencia.class));
    }

    private EmergenciaRequestDTO request() {
        EmergenciaRequestDTO request = new EmergenciaRequestDTO();
        request.setNivelGravedad(NivelGravedad.ALTA);
        request.setZonaAfectada("Zona Norte");
        request.setDescripcion("Desborde del arroyo con familias evacuadas");
        return request;
    }

}
