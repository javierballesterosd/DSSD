package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;

import com.proyecto.backend.client.BonitaMembership;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.client.BonitaSessionInfo;
import com.proyecto.backend.client.BonitaUser;
import com.proyecto.backend.dto.auth.LoginRequest;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.BonitaIntegrationException;
import com.proyecto.backend.exception.InvalidCredentialsException;
import com.proyecto.backend.exception.UnauthenticatedException;
import com.proyecto.backend.model.Municipio;
import com.proyecto.backend.model.Ong;
import com.proyecto.backend.model.Region;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Service
public class AuthService {

    public static final String BONITA_SESSION_ATTRIBUTE = "bonita.session";
    public static final String USER_ATTRIBUTE = "auth.user";
    public static final String MEMBERSHIP_ATTRIBUTE = "auth.membership";

    private final BonitaClient bonitaClient;
    private final OngService ongService;
    private final MunicipioService municipioService;
    private final RegionService regionService;

    public AuthService(BonitaClient bonitaClient, OngService ongService,
                       MunicipioService municipioService, RegionService regionService) {
        this.bonitaClient = bonitaClient;
        this.ongService = ongService;
        this.municipioService = municipioService;
        this.regionService = regionService;
    }

    public LoginResponse login(LoginRequest request, HttpSession httpSession) {
        String username = request.getUsername();
        try {
            LoginResponse response = autenticar(request, httpSession);
            log.info("Login exitoso: usuario='{}', rol={}, grupo='{}', ong/municipio={}",
                    username, response.getRole(), response.getGroup(),
                    response.getOngNombre() != null ? response.getOngNombre() : response.getMunicipioNombre());
            return response;
        } catch (InvalidCredentialsException e) {
            log.warn("Login fallido: usuario='{}', motivo=credenciales inválidas en Bonita", username);
            throw e;
        } catch (BonitaIntegrationException e) {
            log.error("Login fallido: usuario='{}', motivo=error de integración con Bonita ({})",
                    username, e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.error("Login fallido: usuario='{}', motivo=no se pudo consultar Bonita ({})",
                    username, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.warn("Login fallido: usuario='{}', motivo={}", username, e.getMessage());
            throw e;
        }
    }

    private LoginResponse autenticar(LoginRequest request, HttpSession httpSession) {
        BonitaSession bonitaSession = bonitaClient.login(request.getUsername(), request.getPassword());

        BonitaSessionInfo sessionInfo = bonitaClient.getCurrentSession(bonitaSession);
        BonitaUser user = bonitaClient.getUser(bonitaSession, sessionInfo.userId());
        List<BonitaMembership> memberships = bonitaClient.getMemberships(
                bonitaSession,
                sessionInfo.userId()
        );

        if (memberships.size() != 1) {
            throw new IllegalStateException(
                    "El usuario debe tener exactamente un rol y un grupo en Bonita"
            );
        }

        BonitaMembership membership = memberships.get(0);
        LoginResponse response = construirRespuesta(user, membership);

        httpSession.setAttribute(BONITA_SESSION_ATTRIBUTE, bonitaSession);
        httpSession.setAttribute(USER_ATTRIBUTE, user);
        httpSession.setAttribute(MEMBERSHIP_ATTRIBUTE, membership);

        return response;
    }

    public LoginResponse currentUser(HttpSession httpSession) {
        BonitaUser user = (BonitaUser) httpSession.getAttribute(USER_ATTRIBUTE);
        BonitaMembership membership =
                (BonitaMembership) httpSession.getAttribute(MEMBERSHIP_ATTRIBUTE);

        if (user == null || membership == null) {
            throw new UnauthenticatedException();
        }

        return construirRespuesta(user, membership);
    }

    /**
     * Id de la ONG del usuario logueado. Lanza 401 si no hay sesión y 403 si el usuario
     * no es representante de una ONG.
     */
    public Long ongDelUsuario(HttpSession httpSession) {
        LoginResponse usuario = currentUser(httpSession);
        if (!"ONG".equals(usuario.getRole()) || usuario.getOngId() == null) {
            throw new AccesoDenegadoException("Solo un representante de ONG puede registrar ofertas");
        }
        return usuario.getOngId();
    }

    /** Usuario logueado si es auditor. Lanza 401 si no hay sesión y 403 si tiene otro rol. */
    public LoginResponse auditorDelUsuario(HttpSession httpSession) {
        LoginResponse usuario = currentUser(httpSession);
        if (!"AUDITOR".equals(usuario.getRole())) {
            throw new AccesoDenegadoException("Solo un auditor puede ver todas las ofertas");
        }
        return usuario;
    }

    /**
     * Id del municipio del usuario logueado. Lanza 401 si no hay sesión y 403 si el usuario
     * no es operador municipal.
     */
    public Long municipioDelUsuario(HttpSession httpSession) {
        LoginResponse usuario = currentUser(httpSession);
        if (!"MUNICIPAL".equals(usuario.getRole()) || usuario.getMunicipioId() == null) {
            throw new AccesoDenegadoException("Solo un operador municipal puede registrar emergencias");
        }
        return usuario.getMunicipioId();
    }

    /** Sesión de Bonita del usuario logueado (para invocar la API de Bonita en su nombre). */
    public BonitaSession bonitaSession(HttpSession httpSession) {
        BonitaSession session = (BonitaSession) httpSession.getAttribute(BONITA_SESSION_ATTRIBUTE);
        if (session == null) {
            throw new UnauthenticatedException();
        }
        return session;
    }

    public void logout(HttpSession httpSession) {
        httpSession.invalidate();
    }

    private LoginResponse construirRespuesta(BonitaUser user, BonitaMembership membership) {
        String applicationRole = mapRole(membership.role().name());

        LoginResponse response = new LoginResponse();
        response.setUserId(user.id());
        response.setUsername(user.username());
        response.setFirstName(user.firstname());
        response.setLastName(user.lastname());
        response.setRole(applicationRole);
        response.setGroup(membership.group().name());

        // La ONG del representante se resuelve por el path de su subgrupo en Bonita
        if ("ONG".equals(applicationRole)) {
            Ong ong = ongService.obtenerPorGrupoBonita(membership.group().path());
            response.setOngId(ong.getId());
            response.setOngNombre(ong.getRazonSocial());
        }

        // El municipio del operador se resuelve por el path de su subgrupo; el CCR, por el de su región
        if ("MUNICIPAL".equals(applicationRole)) {
            Municipio municipio = municipioService.obtenerPorGrupoBonita(membership.group().path());
            response.setMunicipioId(municipio.getId());
            response.setMunicipioNombre(municipio.getNombre());
            response.setRegionId(municipio.getRegion().getId());
            response.setRegionNombre(municipio.getRegion().getNombre());
        } else if ("COORDINADOR".equals(applicationRole)) {
            Region region = regionService.obtenerPorGrupoBonita(membership.group().path());
            response.setRegionId(region.getId());
            response.setRegionNombre(region.getNombre());
        }
        return response;
    }

    private String mapRole(String bonitaRole) {
        return switch (bonitaRole) {
            case "Operador Municipal" -> "MUNICIPAL";
            case "Representante de ONG" -> "ONG";
            case "Coordinador Regional" -> "COORDINADOR";
            case "Auditor" -> "AUDITOR";
            default -> throw new IllegalStateException(
                    "Rol de Bonita no permitido: " + bonitaRole
            );
        };
    }

}
