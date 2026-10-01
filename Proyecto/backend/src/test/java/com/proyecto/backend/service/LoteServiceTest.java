package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.ItemLoteRequest;
import com.proyecto.backend.dto.LoteRequest;
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
import com.proyecto.backend.repository.EmergenciaRepository;
import com.proyecto.backend.repository.LoteRepository;
import com.proyecto.backend.repository.RecursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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

    private LoteService loteService;
    private Emergencia emergencia;
    private final BonitaSession session = new BonitaSession("session", "token");

    @BeforeEach
    void setUp() {
        ItemLoteMapper itemLoteMapper = new ItemLoteMapper();
        loteService = new LoteService(
                loteRepository, emergenciaRepository, recursoRepository,
                new LoteMapper(itemLoteMapper), itemLoteMapper, bonitaClient);

        emergencia = new Emergencia();
        emergencia.setId(7L);
        emergencia.setBonitaCaseId("caso-7");
        emergencia.setNivelGravedad(NivelGravedad.ALTA);
        Municipio municipio = new Municipio();
        municipio.setNombre("La Plata");
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
                loteService.publicarLote(7L, request(ahora.plusDays(2), ahora.plusDays(1)), session)
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
                loteService.publicarLote(7L, request(ahora, ahora.plusDays(1)), session)
        ).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void completaLaTareaDesglosarEnBonitaConElLoteYSuVentana() {
        emergenciaYRecursoExisten();
        when(bonitaClient.buscarTareaPendiente(session, "caso-7", LoteService.TAREA_DESGLOSAR_LOTES))
                .thenReturn("tarea-9");
        LocalDateTime apertura = LocalDateTime.of(2026, 10, 1, 18, 0);
        LocalDateTime cierre = LocalDateTime.of(2026, 10, 2, 9, 30);

        loteService.publicarLote(7L, request(apertura, cierre), session);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> contrato = ArgumentCaptor.forClass(Map.class);
        verify(bonitaClient).ejecutarTarea(eq(session), eq("tarea-9"), contrato.capture());
        assertThat(contrato.getValue())
                .containsEntry("loteId", 30L)
                .containsEntry("fechaAperturaOfertas", "2026-10-01T18:00:00")
                .containsEntry("fechaCierreOfertas", "2026-10-02T09:30:00");
    }

    @Test
    void siFallaBonitaSePropagaElErrorParaQueSeRevierta()  {
        emergenciaYRecursoExisten();
        when(bonitaClient.buscarTareaPendiente(any(), any(), any())).thenReturn("tarea-9");
        BonitaIntegrationException error = new BonitaIntegrationException("falló");
        doThrow(error).when(bonitaClient).ejecutarTarea(any(), any(), any());
        LocalDateTime ahora = LocalDateTime.now();

        assertThatThrownBy(() ->
                loteService.publicarLote(7L, request(ahora, ahora.plusDays(1)), session)
        ).isSameAs(error);
    }

    @Test
    void rechazaUnaEmergenciaSinCasoEnBonita() {
        emergenciaYRecursoExisten();
        emergencia.setBonitaCaseId(null);
        LocalDateTime ahora = LocalDateTime.now();

        assertThatThrownBy(() ->
                loteService.publicarLote(7L, request(ahora, ahora.plusDays(1)), session)
        ).isInstanceOf(ReglaNegocioException.class);

        verify(bonitaClient, never()).ejecutarTarea(any(), any(), any());
    }
}
