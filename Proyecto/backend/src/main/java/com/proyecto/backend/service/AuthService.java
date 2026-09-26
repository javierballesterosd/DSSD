package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;

import com.proyecto.backend.client.BonitaMembership;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.client.BonitaSessionInfo;
import com.proyecto.backend.client.BonitaUser;
import com.proyecto.backend.dto.auth.LoginRequest;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.exception.UnauthenticatedException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    public static final String BONITA_SESSION_ATTRIBUTE = "bonita.session";
    public static final String USER_ATTRIBUTE = "auth.user";
    public static final String MEMBERSHIP_ATTRIBUTE = "auth.membership";

    private final BonitaClient bonitaClient;

    public AuthService(BonitaClient bonitaClient) {
        this.bonitaClient = bonitaClient;
    }

    public LoginResponse login(LoginRequest request, HttpSession httpSession) {
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
        String applicationRole = mapRole(membership.role().name());

        httpSession.setAttribute(BONITA_SESSION_ATTRIBUTE, bonitaSession);
        httpSession.setAttribute(USER_ATTRIBUTE, user);
        httpSession.setAttribute(MEMBERSHIP_ATTRIBUTE, membership);

        LoginResponse response = new LoginResponse();
        response.setUserId(user.id());
        response.setUsername(user.username());
        response.setFirstName(user.firstname());
        response.setLastName(user.lastname());
        response.setRole(applicationRole);
        response.setGroup(membership.group().name());
        return response;
    }

    public LoginResponse currentUser(HttpSession httpSession) {
        BonitaUser user = (BonitaUser) httpSession.getAttribute(USER_ATTRIBUTE);
        BonitaMembership membership =
                (BonitaMembership) httpSession.getAttribute(MEMBERSHIP_ATTRIBUTE);

        if (user == null || membership == null) {
            throw new UnauthenticatedException();
        }

        LoginResponse response = new LoginResponse();
        response.setUserId(user.id());
        response.setUsername(user.username());
        response.setFirstName(user.firstname());
        response.setLastName(user.lastname());
        response.setRole(mapRole(membership.role().name()));
        response.setGroup(membership.group().name());
        return response;
    }

    public void logout(HttpSession httpSession) {
        httpSession.invalidate();
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
