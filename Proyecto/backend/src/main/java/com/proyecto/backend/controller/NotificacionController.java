package com.proyecto.backend.controller;

import com.proyecto.backend.dto.NotificacionResponse;
import com.proyecto.backend.service.NotificacionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public List<NotificacionResponse> getNotificaciones(HttpSession session) {
        return notificacionService.listarVisibles(session);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotificacion(HttpSession session, @PathVariable Long id) {
        notificacionService.eliminar(id, session);
    }
}
