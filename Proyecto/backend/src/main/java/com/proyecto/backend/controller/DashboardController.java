package com.proyecto.backend.controller;

import com.proyecto.backend.dto.DashboardRankingResponse;
import com.proyecto.backend.dto.DashboardBonitaResponse.*;
import com.proyecto.backend.service.DashboardService;
import com.proyecto.backend.model.NivelGravedad;
import com.proyecto.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Rankings históricos locales. Sin límite se devuelven todas las entidades, incluso con total cero. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;

    @GetMapping("/ongs/ofertas")
    public List<DashboardRankingResponse> ongsPorOfertas(HttpSession session,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.ongsPorOfertas(limite, desde, hasta);
    }

    @GetMapping("/ongs/recursos-asignados")
    public List<DashboardRankingResponse> ongsPorRecursosAsignados(HttpSession session,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.ongsPorRecursosAsignados(limite, desde, hasta);
    }

    @GetMapping("/municipios/emergencias")
    public List<DashboardRankingResponse> municipiosPorEmergencias(HttpSession session,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.municipiosPorEmergencias(limite, desde, hasta);
    }

    @GetMapping("/recursos/necesidad")
    public List<DashboardRankingResponse> recursosPorNecesidad(HttpSession session,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.recursosPorNecesidad(limite, desde, hasta);
    }

    @GetMapping("/recursos/cantidad")
    public List<DashboardRankingResponse> recursosPorCantidad(HttpSession session,
            @RequestParam(required = false) Integer limite) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.recursosPorCantidad(limite);
    }

    @GetMapping("/etapas/tiempos")
    public List<Tiempo> etapas(HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) Long municipioId,
            @RequestParam(required = false) NivelGravedad gravedad,
            @RequestParam(required = false) Long recursoId,
            @RequestParam(required = false) Long ongId) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.etapas(authService.bonitaSession(session), desde, hasta, limite, municipioId, gravedad, recursoId, ongId);
    }

    @GetMapping("/etapas/ranking")
    public List<RankingEtapa> rankingEtapas(HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) Long municipioId,
            @RequestParam(required = false) NivelGravedad gravedad,
            @RequestParam(required = false) Long recursoId,
            @RequestParam(required = false) Long ongId) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.rankingEtapas(authService.bonitaSession(session), desde, hasta, limite, municipioId, gravedad, recursoId, ongId);
    }

    @GetMapping("/tiempos/validacion-nacional")
    public Tiempo validacion(HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long municipioId,
            @RequestParam(required = false) NivelGravedad gravedad,
            @RequestParam(required = false) Long recursoId,
            @RequestParam(required = false) Long ongId) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.validacion(authService.bonitaSession(session), desde, hasta, municipioId, gravedad, recursoId, ongId);
    }

    @GetMapping("/tiempos/decision-municipal")
    public Tiempo decision(HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long municipioId,
            @RequestParam(required = false) NivelGravedad gravedad,
            @RequestParam(required = false) Long recursoId,
            @RequestParam(required = false) Long ongId) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.tarea(authService.bonitaSession(session), "Ver ofertas validadas", desde, hasta, municipioId, gravedad, recursoId, ongId);
    }

    @GetMapping("/tiempos/compromiso")
    public Tiempo compromiso(HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long municipioId,
            @RequestParam(required = false) NivelGravedad gravedad,
            @RequestParam(required = false) Long recursoId,
            @RequestParam(required = false) Long ongId) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.tarea(authService.bonitaSession(session), "Registrar compromiso y bloqueo", desde, hasta, municipioId, gravedad, recursoId, ongId);
    }

    @GetMapping("/tiempos/emergencias-finalizadas")
    public Tiempo finalizadas(HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long municipioId,
            @RequestParam(required = false) NivelGravedad gravedad,
            @RequestParam(required = false) Long recursoId,
            @RequestParam(required = false) Long ongId) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.finalizadas(authService.bonitaSession(session), desde, hasta, municipioId, gravedad, recursoId, ongId);
    }

    @GetMapping("/emergencias/seguimiento")
    public List<Seguimiento> seguimiento(HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long municipioId,
            @RequestParam(required = false) NivelGravedad gravedad,
            @RequestParam(required = false) Long recursoId,
            @RequestParam(required = false) Long ongId) {
        authService.requerirRol(session, "AUDITOR");
        return dashboardService.seguimiento(authService.bonitaSession(session), desde, hasta, municipioId, gravedad, recursoId, ongId);
    }
}
