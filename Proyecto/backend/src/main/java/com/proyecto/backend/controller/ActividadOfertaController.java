package com.proyecto.backend.controller;

import com.proyecto.backend.dto.MiDespliegueResponse;
import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.service.ActividadOfertaService;
import com.proyecto.backend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pantalla de seguimiento del representante de ONG: sus ofertas adjudicadas y el avance de las actividades. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActividadOfertaController {

    private final ActividadOfertaService service;
    private final AuthService auth;

    @GetMapping("/despliegues/mios")
    public List<MiDespliegueResponse> misDespliegues(HttpSession sesion) {
        return service.misDespliegues(ongDelUsuario(sesion).getOngId());
    }

    @PostMapping("/ofertas/{ofertaId}/actividades/{actividadId}/finalizacion")
    public MiDespliegueResponse.Actividad finalizar(@PathVariable Long ofertaId, @PathVariable Long actividadId,
            HttpSession sesion) {
        LoginResponse usuario = ongDelUsuario(sesion);
        return service.finalizar(ofertaId, actividadId, usuario.getOngId(), usuario.getUsername(),
                auth.bonitaSession(sesion));
    }

    private LoginResponse ongDelUsuario(HttpSession sesion) {
        LoginResponse usuario = auth.requerirRol(sesion, "ONG");
        if (usuario.getOngId() == null) throw new AccesoDenegadoException("Tu usuario no tiene una ONG asociada");
        return usuario;
    }
}
