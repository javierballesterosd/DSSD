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
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.exception.UnauthenticatedException;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.model.Ong;
import com.proyecto.backend.model.Region;
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
    private MunicipioService municipioService;
    @Mock
    private RegionService regionService;
    @Mock
    private HttpSession httpSession;

    private AuthService authService;
    private final BonitaSession bonitaSession = new BonitaSession("jsession", "token");

    @BeforeEach
    void setUp() {
        authService = new AuthService(bonitaClient, ongService, municipioService, regionService);
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
        when(ongService.obtenerPorGrupoBonitaEnJerarquia("/ONG/CaritasBuenosAires")).thenReturn(ong);

        LoginResponse response = authService.login(request("ong.caritas"), httpSession);

        assertThat(response.getRole()).isEqualTo("ONG");
        assertThat(response.getOngId()).isEqualTo(2L);
        assertThat(response.getOngNombre()).isEqualTo("Cáritas Arquidiócesis de Buenos Aires");
    }

    @Test
    void loginDeOperadorMunicipalDevuelveMunicipioYRegionVinculadosPorPath() {
        BonitaGroup grupo = new BonitaGroup("11", "LaPlata", "La Plata", "/Municipio/Region1");
        bonitaDevuelve("operador.laplata", "Operador Municipal", grupo);
        Region region = new Region();
        region.setId(1L);
        region.setNombre("Región 1");
        Municipio municipio = new Municipio();
        municipio.setId(5L);
        municipio.setNombre("La Plata");
        municipio.setRegion(region);
        when(municipioService.obtenerPorGrupoBonitaEnJerarquia("/Municipio/Region1/LaPlata")).thenReturn(municipio);

        LoginResponse response = authService.login(request("operador.laplata"), httpSession);

        assertThat(response.getRole()).isEqualTo("MUNICIPAL");
        assertThat(response.getMunicipioId()).isEqualTo(5L);
        assertThat(response.getMunicipioNombre()).isEqualTo("La Plata");
        assertThat(response.getRegionId()).isEqualTo(1L);
        assertThat(response.getRegionNombre()).isEqualTo("Región 1");
        assertThat(response.getOngId()).isNull();
    }

    @Test
    void loginDeCoordinadorDevuelveSuRegion() {
        BonitaGroup grupo = new BonitaGroup("12", "Region1", "Región 1", "/Municipio");
        bonitaDevuelve("coord.norte", "Coordinador Regional", grupo);
        Region region = new Region();
        region.setId(1L);
        region.setNombre("Región 1");
        when(regionService.obtenerPorGrupoBonitaEnJerarquia("/Municipio/Region1")).thenReturn(region);

        LoginResponse response = authService.login(request("coord.norte"), httpSession);

        assertThat(response.getRole()).isEqualTo("COORDINADOR");
        assertThat(response.getRegionId()).isEqualTo(1L);
        assertThat(response.getRegionNombre()).isEqualTo("Región 1");
        assertThat(response.getMunicipioId()).isNull();
        assertThat(response.getOngId()).isNull();
    }

    @Test
    void loginDeAuditorNoResuelveOrganizacion() {
        BonitaGroup grupo = new BonitaGroup("13", "Sistema Nacional", "Sistema Nacional", null);
        bonitaDevuelve("auditor.nacional", "Auditor", grupo);

        LoginResponse response = authService.login(request("auditor.nacional"), httpSession);

        assertThat(response.getRole()).isEqualTo("AUDITOR");
        assertThat(response.getOngId()).isNull();
        assertThat(response.getMunicipioId()).isNull();
        assertThat(response.getRegionId()).isNull();
    }

    @Test
    void loginDeSubgrupoSinOngSembradaFalla() {
        BonitaGroup grupo = new BonitaGroup("56", "OngNueva", "Ong nueva", "/ONG");
        bonitaDevuelve("ong.nueva", "Representante de ONG", grupo);
        when(ongService.obtenerPorGrupoBonitaEnJerarquia("/ONG/OngNueva"))
                .thenThrow(new RecursoNoEncontradoException("No hay una ONG registrada"));

        assertThatThrownBy(() -> authService.login(request("ong.nueva"), httpSession))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void pathDelGrupoSinPadreEsSoloElNombre() {
        assertThat(new BonitaGroup("1", "Municipio", "Municipio", null).path()).isEqualTo("/Municipio");
    }

    private void sesionCon(String rol, BonitaGroup grupo) {
        when(httpSession.getAttribute(AuthService.USER_ATTRIBUTE))
                .thenReturn(new BonitaUser("7", "usuario", "Elena", "Rojas"));
        when(httpSession.getAttribute(AuthService.MEMBERSHIP_ATTRIBUTE))
                .thenReturn(new BonitaMembership("7", new BonitaRole("1", rol, rol), grupo));
    }

    @Test
    void ongDelUsuarioDevuelveLaOngDelRepresentante() {
        sesionCon("Representante de ONG", new BonitaGroup("55", "CaritasBuenosAires", "Cáritas", "/ONG"));
        Ong ong = new Ong();
        ong.setId(2L);
        ong.setRazonSocial("Cáritas Arquidiócesis de Buenos Aires");
        when(ongService.obtenerPorGrupoBonitaEnJerarquia("/ONG/CaritasBuenosAires")).thenReturn(ong);

        assertThat(authService.ongDelUsuario(httpSession)).isEqualTo(2L);
    }

    @Test
    void ongDelUsuarioRechazaOtrosRoles() {
        sesionCon("Coordinador Regional", new BonitaGroup("12", "Region1", "Región 1", "/Municipio"));
        Region region = new Region();
        region.setId(1L);
        region.setNombre("Región 1");
        when(regionService.obtenerPorGrupoBonitaEnJerarquia("/Municipio/Region1")).thenReturn(region);

        assertThatThrownBy(() -> authService.ongDelUsuario(httpSession))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void municipioDelUsuarioDevuelveElMunicipioDelOperador() {
        sesionCon("Operador Municipal", new BonitaGroup("11", "LaPlata", "La Plata", "/Municipio/Region1"));
        Region region = new Region();
        region.setId(1L);
        region.setNombre("Región 1");
        Municipio municipio = new Municipio();
        municipio.setId(5L);
        municipio.setNombre("La Plata");
        municipio.setRegion(region);
        when(municipioService.obtenerPorGrupoBonitaEnJerarquia("/Municipio/Region1/LaPlata")).thenReturn(municipio);

        assertThat(authService.municipioDelUsuario(httpSession)).isEqualTo(5L);
    }

    @Test
    void municipioDelUsuarioRechazaOtrosRoles() {
        sesionCon("Auditor", new BonitaGroup("13", "Sistema Nacional", "Sistema Nacional", null));

        assertThatThrownBy(() -> authService.municipioDelUsuario(httpSession))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    private void sesionDeCoordinador() {
        sesionCon("Coordinador Regional", new BonitaGroup("12", "Region1", "Región 1", "/Municipio"));
        Region region = new Region();
        region.setId(1L);
        region.setNombre("Región 1");
        when(regionService.obtenerPorGrupoBonitaEnJerarquia("/Municipio/Region1")).thenReturn(region);
    }

    @Test
    void regionDelUsuarioDevuelveLaRegionDelCoordinador() {
        sesionDeCoordinador();

        assertThat(authService.regionDelUsuario(httpSession)).isEqualTo(1L);
    }

    @Test
    void regionDelUsuarioRechazaOtrosRoles() {
        sesionCon("Auditor", new BonitaGroup("13", "Sistema Nacional", "Sistema Nacional", null));

        assertThatThrownBy(() -> authService.regionDelUsuario(httpSession))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void requerirRolDevuelveElUsuarioSiTieneUnoDeLosRoles() {
        sesionDeCoordinador();

        assertThat(authService.requerirRol(httpSession, "ONG", "COORDINADOR").getRole())
                .isEqualTo("COORDINADOR");
    }

    @Test
    void requerirRolRechazaUnRolNoPermitido() {
        sesionDeCoordinador();

        assertThatThrownBy(() -> authService.requerirRol(httpSession, "ONG", "AUDITOR"))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void ongDelUsuarioSinSesionFalla() {
        assertThatThrownBy(() -> authService.ongDelUsuario(httpSession))
                .isInstanceOf(UnauthenticatedException.class);
    }
}
