package com.proyecto.backend.controller;

import com.proyecto.backend.dto.notificacion.NotificacionResponseDTO;
import com.proyecto.backend.service.NotificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public List<NotificacionResponseDTO> getNotificaciones(HttpSession session) {
        return notificacionService.listarVisibles(session);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotificacion(HttpSession session, @PathVariable Long id) {
        notificacionService.eliminar(id, session);
    }
}
