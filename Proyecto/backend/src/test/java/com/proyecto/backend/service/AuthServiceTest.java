package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaGroup;
import com.proyecto.backend.client.BonitaMembership;
import com.proyecto.backend.client.BonitaRole;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.client.BonitaSessionInfo;
import com.proyecto.backend.client.BonitaUser;
import com.proyecto.backend.dto.auth.LoginRequest;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.model.Ong;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private BonitaClient bonitaClient;
    @Mock
    private OngService ongService;
    @Mock
    private HttpSession httpSession;

    private AuthService authService;
    private final BonitaSession bonitaSession = new BonitaSession("jsession", "token");

    @BeforeEach
    void setUp() {
        authService = new AuthService(bonitaClient, ongService);
    }

    private LoginRequest request(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("bpm");
        return request;
    }

    private void bonitaDevuelve(String username, String rol, BonitaGroup grupo) {
        when(bonitaClient.login(username, "bpm")).thenReturn(bonitaSession);
        when(bonitaClient.getCurrentSession(bonitaSession)).thenReturn(new BonitaSessionInfo("7"));
        when(bonitaClient.getUser(bonitaSession, "7"))
                .thenReturn(new BonitaUser("7", username, "Elena", "Rojas"));
        when(bonitaClient.getMemberships(bonitaSession, "7")).thenReturn(List.of(
                new BonitaMembership("7", new BonitaRole("1", rol, rol), grupo)));
    }

    @Test
    void loginDeRepresentanteDeOngDevuelveLaOngVinculadaPorPathDelGrupo() {
        BonitaGroup grupo = new BonitaGroup("55", "CaritasBuenosAires", "Cáritas", "/ONG");
        bonitaDevuelve("ong.caritas", "Representante de ONG", grupo);
        Ong ong = new Ong();
        ong.setId(2L);
        ong.setRazonSocial("Cáritas Arquidiócesis de Buenos Aires");
        when(ongService.obtenerPorGrupoBonita("/ONG/CaritasBuenosAires")).thenReturn(ong);

        LoginResponse response = authService.login(request("ong.caritas"), httpSession);

        assertThat(response.getRole()).isEqualTo("ONG");
        assertThat(response.getOngId()).isEqualTo(2L);
        assertThat(response.getOngNombre()).isEqualTo("Cáritas Arquidiócesis de Buenos Aires");
    }

    @Test
    void loginDeOtroRolNoResuelveOng() {
        BonitaGroup grupo = new BonitaGroup("10", "Municipio", "Municipio", null);
        bonitaDevuelve("operador.laplata", "Operador Municipal", grupo);

        LoginResponse response = authService.login(request("operador.laplata"), httpSession);

        assertThat(response.getRole()).isEqualTo("MUNICIPAL");
        assertThat(response.getOngId()).isNull();
        assertThat(response.getOngNombre()).isNull();
    }

    @Test
    void loginDeSubgrupoSinOngSembradaFalla() {
        BonitaGroup grupo = new BonitaGroup("56", "OngNueva", "Ong nueva", "/ONG");
        bonitaDevuelve("ong.nueva", "Representante de ONG", grupo);
        when(ongService.obtenerPorGrupoBonita("/ONG/OngNueva"))
                .thenThrow(new RecursoNoEncontradoException("No hay una ONG registrada"));

        assertThatThrownBy(() -> authService.login(request("ong.nueva"), httpSession))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void pathDelGrupoSinPadreEsSoloElNombre() {
        assertThat(new BonitaGroup("1", "Municipio", "Municipio", null).path()).isEqualTo("/Municipio");
    }
}
