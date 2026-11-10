package com.proyecto.backend.service;

import com.proyecto.backend.client.*;
import com.proyecto.backend.dto.DashboardBonitaResponse.*;
import com.proyecto.backend.repository.DashboardRepository;
import com.proyecto.backend.exception.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.LocalDate;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class DashboardBonitaServiceTest {
    private final BonitaClient bonita = mock(BonitaClient.class);
    private final DashboardRepository repository = mock(DashboardRepository.class);
    private final BonitaSession session = new BonitaSession("session", "token");
    private final DashboardService service = new DashboardService(repository, bonita);

    private void locales() {
        when(repository.emergenciasConCaso(null, null, null, null)).thenReturn(List.of(new EmergenciaLocal(12L, "6", "Municipio", "Emergencia")));
    }
    private Map<String,Object> nodo(String id, String tarea, String estado, String fecha) {
        return Map.of("sourceObjectId", id, "caseId", "6", "type", "USER_TASK", "name", tarea,
                "state", estado, "archivedDate", fecha);
    }
    @Test void cuentaCadaRepeticionUnaVezYExcluyePendientesYCuentasAjenas() {
        locales();
        when(bonita.datosDashboard(session,"archivedFlowNode")).thenReturn(List.of(
                nodo("1","Ver ofertas validadas","ready","2026-10-01 10:00:00.000"),
                nodo("1","Ver ofertas validadas","completed","2026-10-01 10:02:00.000"),
                nodo("1","Ver ofertas validadas","completed","2026-10-01 10:02:00.000"),
                nodo("2","Ver ofertas validadas","ready","2026-10-02 10:00:00.000"),
                nodo("2","Ver ofertas validadas","completed","2026-10-02 10:04:00.000"),
                nodo("3","Pendiente","ready","2026-10-01 10:00:00.000"),
                Map.of("caseId","otro")));
        var total=service.tarea(session,"Ver ofertas validadas",null,null, null, null, null, null);
        assertThat(total.cantidad()).isEqualTo(2);
        assertThat(total.promedioSegundos()).isEqualTo(180.0);
        var filtrado=service.etapas(session,LocalDate.of(2026,10,2),LocalDate.of(2026,10,2),1, null, null, null, null);
        assertThat(filtrado).hasSize(1);
        assertThat(filtrado.getFirst().promedioSegundos()).isEqualTo(240.0);
    }
    @Test void noInventaDuracionesCuandoFaltaElInicio() {
        locales();
        when(bonita.datosDashboard(session,"archivedFlowNode")).thenReturn(List.of(
                nodo("1","Tarea","completed","2026-10-01 10:02:00.000")));
        var tiempo=service.tarea(session,"Tarea",null,null, null, null, null, null);
        assertThat(tiempo.cantidad()).isZero();assertThat(tiempo.promedioSegundos()).isNull();
    }
    @Test void mideHastaLaPrimeraValidacionDesdeElInicioDelCaso() {
        locales();
        String tarea="Validar en el Sistema Nacional (POST Cloud)";
        when(bonita.datosDashboard(session,"archivedFlowNode")).thenReturn(List.of(
                nodo("1",tarea,"initializing","2026-10-01 10:01:00.000"),
                nodo("1",tarea,"completed","2026-10-01 10:02:00.000"),
                nodo("2",tarea,"initializing","2026-10-02 10:00:00.000"),
                nodo("2",tarea,"completed","2026-10-02 10:02:00.000")));
        when(bonita.datosDashboard(session,"case")).thenReturn(List.of(Map.of("id","6","start","2026-10-01 10:00:00.000")));
        assertThat(service.validacion(session,null,null, null, null, null, null).promedioSegundos()).isEqualTo(120.0);
    }
    @Test void finalizadasExcluyeCasosCanceladosYDeduplicaEstadosArchivados() {
        locales();
        var completado=Map.<String,Object>of("sourceObjectId","6","state","completed",
                "start","2026-10-01 10:00:00.000","end_date","2026-10-02 10:00:00.000");
        when(bonita.datosDashboard(session,"archivedCase")).thenReturn(List.of(
                Map.of("sourceObjectId","6","state","cancelled"),completado,completado));
        assertThat(service.finalizadas(session,null,null, null, null, null, null)).isEqualTo(new Tiempo("Emergencias finalizadas",1,86400.0));
    }
    @Test void seguimientoCombinaLosDatosLocalesConLaEtapaActual() {
        locales();
        when(bonita.datosDashboard(session,"case")).thenReturn(List.of(Map.of("id","6","start","2026-10-01 10:00:00.000")));
        when(bonita.datosDashboard(session,"activity")).thenReturn(List.of(
                Map.of("caseId","6","name","Monitorear despliegue","state","ready"),
                Map.of("caseId","otro","name","Monitorear despliegue","state","ready")));
        assertThat(service.seguimiento(session,null,null, null, null, null, null)).containsExactly(
                new Seguimiento(12L,"6","Municipio","Emergencia","Monitorear despliegue","ready"));
    }
    @Test void losSeisEndpointsExigenAuditorAntesDeConsultarBonita() {
        var auth = mock(AuthService.class);
        var reportes = mock(DashboardService.class);
        var http = mock(jakarta.servlet.http.HttpSession.class);
        var controller = new com.proyecto.backend.controller.DashboardController(reportes, auth);
        when(auth.requerirRol(http, "AUDITOR")).thenThrow(new AccesoDenegadoException("Sin permiso"));
        assertThatThrownBy(() -> controller.etapas(http, null, null, null, null, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.validacion(http, null, null, null, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.decision(http, null, null, null, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.compromiso(http, null, null, null, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.finalizadas(http, null, null, null, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.seguimiento(http, null, null, null, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(reportes);
    }

    @Test void fechasInvalidasNoConsultanBonitaYLasFallasNoSeConviertenEnCeros() {
        assertThatThrownBy(()->service.etapas(session,LocalDate.of(2026,11,2),LocalDate.of(2026,11,1),null, null, null, null, null))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(bonita,repository);
        locales();
        when(bonita.datosDashboard(session,"archivedFlowNode")).thenThrow(new BonitaIntegrationException("Sin conexion"));
        assertThatThrownBy(()->service.etapas(session,null,null,null, null, null, null, null)).isInstanceOf(BonitaIntegrationException.class);
    }
    @Test void filtraLosCasosLocalesAntesDeCalcularElRankingYLasOtrasMetricas() {
        var gravedad = com.proyecto.backend.model.NivelGravedad.ALTA;
        when(repository.emergenciasConCaso(2L, gravedad, 3L, 4L)).thenReturn(List.of(
                new EmergenciaLocal(12L, "6", "Municipio", "Emergencia")));
        when(bonita.datosDashboard(session, "archivedFlowNode")).thenReturn(List.of(
                nodo("1", "Rapida", "ready", "2026-10-01 10:00:00.000"),
                nodo("1", "Rapida", "completed", "2026-10-01 10:01:00.000"),
                nodo("2", "Lenta", "ready", "2026-10-01 10:00:00.000"),
                nodo("2", "Lenta", "completed", "2026-10-01 10:05:00.000"),
                Map.of("caseId", "otro", "type", "USER_TASK", "sourceObjectId", "9",
                        "name", "Fuera del filtro", "state", "completed")));
        var ranking = service.rankingEtapas(session, null, null, null, 2L, gravedad, 3L, 4L);
        assertThat(ranking).containsExactly(new RankingEtapa(1, "Lenta", 1, 300.0),
                new RankingEtapa(2, "Rapida", 1, 60.0));
        assertThat(service.rankingEtapas(session, null, null, 1, 2L, gravedad, 3L, 4L)).hasSize(1);
        service.tarea(session, "Rapida", null, null, 2L, gravedad, 3L, 4L);
        service.validacion(session, null, null, 2L, gravedad, 3L, 4L);
        service.finalizadas(session, null, null, 2L, gravedad, 3L, 4L);
        service.seguimiento(session, null, null, 2L, gravedad, 3L, 4L);
        verify(repository, times(6)).emergenciasConCaso(2L, gravedad, 3L, 4L);
        verify(repository, never()).emergenciasConCaso(null, null, null, null);
    }

    @Test void rechazaMunicipiosInvalidosAntesDeConsultar() {
        assertThatThrownBy(() -> service.rankingEtapas(session, null, null, null, 0L, null, null, null))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> service.rankingEtapas(session, null, null, null, null, null, -1L, null))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> service.rankingEtapas(session, null, null, null, null, null, null, 0L))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(repository, bonita);
    }
}
