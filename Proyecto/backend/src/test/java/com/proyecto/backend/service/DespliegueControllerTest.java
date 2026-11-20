package com.proyecto.backend.service;

import com.proyecto.backend.controller.DespliegueController;
import com.proyecto.backend.dto.IntegracionNacional;
import com.proyecto.backend.exception.*;
import com.proyecto.backend.security.BonitaTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DespliegueControllerTest {
    @Test void integracionNacionalExigeElTokenDeBonita() {
        var servicio = mock(DespliegueService.class);
        var auth = mock(AuthService.class);
        var tokens = mock(BonitaTokenValidator.class);
        var controller = new DespliegueController(servicio, auth, tokens);
        assertThatThrownBy(() -> controller.plan(1L, 2L, "3", null)).isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(() -> controller.confirmar(1L, new IntegracionNacional.Respuesta(2L, "3", List.of()), "Bearer otro"))
                .isInstanceOf(UnauthenticatedException.class);
        verifyNoInteractions(servicio);
    }
    @Test void monitoreoYValidacionExigenUnCoordinador() {
        var servicio = mock(DespliegueService.class);
        var auth = mock(AuthService.class);
        var controller = new DespliegueController(servicio, auth, mock(BonitaTokenValidator.class));
        var sesion = new MockHttpSession();
        when(auth.regionDelUsuario(sesion)).thenThrow(new AccesoDenegadoException("Solo coordinadores"));
        assertThatThrownBy(() -> controller.completar(1L, sesion)).isInstanceOf(AccesoDenegadoException.class);
        assertThatThrownBy(() -> controller.validar(1L, sesion)).isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(servicio);
    }
}
