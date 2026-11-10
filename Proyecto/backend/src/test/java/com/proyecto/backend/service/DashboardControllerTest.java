package com.proyecto.backend.service;

import com.proyecto.backend.controller.DashboardController;
import com.proyecto.backend.dto.DashboardRankingResponse;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.repository.DashboardRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardControllerTest {
    private final DashboardRepository repository = mock(DashboardRepository.class);
    private final AuthService auth = mock(AuthService.class);
    private final HttpSession session = mock(HttpSession.class);
    private final DashboardController controller = new DashboardController(new DashboardService(repository, mock(com.proyecto.backend.client.BonitaClient.class)), auth);

    @Test void todosLosRankingsRequierenAuditorAntesDeConsultarDatos() {
        when(auth.requerirRol(session, "AUDITOR")).thenThrow(new AccesoDenegadoException("Sin permiso"));
        assertThatThrownBy(() -> controller.ongsPorOfertas(session, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.ongsPorRecursosAsignados(session, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.municipiosPorEmergencias(session, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.recursosPorNecesidad(session, null, null, null)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.recursosPorCantidad(session, null)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(repository);
    }

    @Test void permiteObtenerElRankingCompletoOUnTopSinCambiarSuOrden() {
        var ranking = List.of(new DashboardRankingResponse(2L, "A", 10, null),
                new DashboardRankingResponse(1L, "B", 4, null));
        when(repository.municipiosPorEmergencias(null, null)).thenReturn(ranking);
        assertThat(controller.municipiosPorEmergencias(session, null, null, null)).isEqualTo(ranking);
        assertThat(controller.municipiosPorEmergencias(session, 1, null, null)).containsExactly(ranking.getFirst());
        assertThat(controller.municipiosPorEmergencias(session, 5, null, null)).isEqualTo(ranking);
    }

    @Test void rechazaFechasInvertidasAntesDeConsultar() {
        assertThatThrownBy(() -> controller.municipiosPorEmergencias(session, null,
                java.time.LocalDate.of(2026, 11, 2), java.time.LocalDate.of(2026, 11, 1)))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(repository);
    }

    @Test void rechazaLimitesInvalidosAntesDeConsultar() {
        assertThatThrownBy(() -> controller.ongsPorOfertas(session, 0, null, null)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> controller.recursosPorCantidad(session, -1)).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(repository);
    }
}
