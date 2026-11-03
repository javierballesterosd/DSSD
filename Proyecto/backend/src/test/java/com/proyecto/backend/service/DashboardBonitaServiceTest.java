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
        when(repository.emergenciasConCaso()).thenReturn(List.of(new EmergenciaLocal(12L, "6", "Municipio", "Emergencia")));
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
        var total=service.tarea(session,"Ver ofertas validadas",null,null);
        assertThat(total.cantidad()).isEqualTo(2);
        assertThat(total.promedioSegundos()).isEqualTo(180.0);
        var filtrado=service.etapas(session,LocalDate.of(2026,10,2),LocalDate.of(2026,10,2),1);
        assertThat(filtrado).hasSize(1);
        assertThat(filtrado.getFirst().promedioSegundos()).isEqualTo(240.0);
    }
    @Test void noInventaDuracionesCuandoFaltaElInicio() {
        locales();
        when(bonita.datosDashboard(session,"archivedFlowNode")).thenReturn(List.of(
                nodo("1","Tarea","completed","2026-10-01 10:02:00.000")));
        var tiempo=service.tarea(session,"Tarea",null,null);
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
        assertThat(service.validacion(session,null,null).promedioSegundos()).isEqualTo(120.0);
    }
    @Test void finalizadasExcluyeCasosCanceladosYDeduplicaEstadosArchivados() {
        locales();
        var completado=Map.<String,Object>of("sourceObjectId","6","state","completed",
                "start","2026-10-01 10:00:00.000","end_date","2026-10-02 10:00:00.000");
        when(bonita.datosDashboard(session,"archivedCase")).thenReturn(List.of(
                Map.of("sourceObjectId","6","state","cancelled"),completado,completado));
        assertThat(service.finalizadas(session,null,null)).isEqualTo(new Tiempo("Emergencias finalizadas",1,86400.0));
    }
    @Test void seguimientoCombinaLosDatosLocalesConLaEtapaActual() {
        locales();
        when(bonita.datosDashboard(session,"case")).thenReturn(List.of(Map.of("id","6","start","2026-10-01 10:00:00.000")));
        when(bonita.datosDashboard(session,"activity")).thenReturn(List.of(
                Map.of("caseId","6","name","Monitorear despliegue","state","ready"),
                Map.of("caseId","otro","name","Monitorear despliegue","state","ready")));
        assertThat(service.seguimiento(session,null,null)).containsExactly(
                new Seguimiento(12L,"6","Municipio","Emergencia","Monitorear despliegue","ready"));
    }
    @Test void losSeisEndpointsExigenAuditorAntesDeConsultarBonita() {
        var auth = mock(AuthService.class);
        var reportes = mock(DashboardService.class);
        var http = mock(jakarta.servlet.http.HttpSession.class);
        var controller = new com.proyecto.backend.controller.DashboardController(reportes, auth);
        when(auth.requerirRol(http, "AUDITOR")).thenThrow(new AccesoDenegadoException("Sin permiso"));
        assertThatThrownBy(() -> controller.etapas(http, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.validacion(http, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.decision(http, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.compromiso(http, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.finalizadas(http, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.seguimiento(http, null, null)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(reportes);
    }

    @Test void fechasInvalidasNoConsultanBonitaYLasFallasNoSeConviertenEnCeros() {
        assertThatThrownBy(()->service.etapas(session,LocalDate.of(2026,11,2),LocalDate.of(2026,11,1),null))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(bonita,repository);
        locales();
        when(bonita.datosDashboard(session,"archivedFlowNode")).thenThrow(new BonitaIntegrationException("Sin conexion"));
        assertThatThrownBy(()->service.etapas(session,null,null,null)).isInstanceOf(BonitaIntegrationException.class);
    }
}
