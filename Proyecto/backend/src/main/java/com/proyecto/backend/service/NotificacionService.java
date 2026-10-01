package com.proyecto.backend.service;

import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.DescriptorAudiencia;
import com.proyecto.backend.dto.NotificacionResponse;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.model.Notificacion;
import com.proyecto.backend.model.NotificacionInactiva;
import com.proyecto.backend.repository.NotificacionInactivaRepository;
import com.proyecto.backend.repository.NotificacionRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionInactivaRepository notificacionInactivaRepository;
    private final AuthService authService;

    /**
     * Publica una notificación usando el usuario autenticado como remitente.
     */
    @Transactional
    public NotificacionResponse crear(
            String rolDestinatario,
            DescriptorAudiencia audiencia,
            String titulo,
            String descripcion,
            HttpSession session
    ) {
        LoginResponse remitente = authService.currentUser(session);
        return crear(rolDestinatario, audiencia, titulo, descripcion, remitente);
    }

    /**
     * Publica una notificación para ser invocado por otro service del backend.
     */
    @Transactional
    public NotificacionResponse crear(
            String rolDestinatario,
            DescriptorAudiencia audiencia,
            String titulo,
            String descripcion,
            LoginResponse remitente
    ) {
        try {
            validarDatosDeCreacion(rolDestinatario, audiencia, titulo, descripcion, remitente);

            Notificacion notificacion = new Notificacion();
            notificacion.setRolDestinatario(rolDestinatario);
            notificacion.setGrupoDestinatario(audiencia.grupoDestinatario());
            notificacion.setTitulo(titulo);
            notificacion.setDescripcion(descripcion);
            notificacion.setRemitenteUserId(remitente.getUserId());
            notificacion.setRemitenteUsername(remitente.getUsername());

            Notificacion guardada = notificacionRepository.save(notificacion);
            log.info(
                    "Notificación creada: id={}, remitente={}, rolDestinatario={}, grupoDestinatario={}",
                    guardada.getId(),
                    guardada.getRemitenteUserId(),
                    guardada.getRolDestinatario(),
                    guardada.getGrupoDestinatario()
            );
            return toResponse(guardada);
        } catch (RuntimeException exception) {
            log.error(
                    "Error al crear notificación: remitente={}, rolDestinatario={}, grupoDestinatario={}",
                    remitente != null ? remitente.getUserId() : null,
                    rolDestinatario,
                    audiencia != null ? audiencia.grupoDestinatario() : null,
                    exception
            );
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarVisibles(HttpSession session) {
        LoginResponse usuario = authService.currentUser(session);
        try {
            return notificacionRepository.findVisiblesParaUsuario(
                            usuario.getRole(),
                            usuario.getGroupPath(),
                            usuario.getUserId()
                    ).stream()
                    .map(this::toResponse)
                    .toList();
        } catch (RuntimeException exception) {
            log.error(
                    "Error al listar notificaciones: usuario={}, rol={}, grupo={}",
                    usuario.getUserId(),
                    usuario.getRole(),
                    usuario.getGroupPath(),
                    exception
            );
            throw exception;
        }
    }

    /**
     * Descarta una notificación solo para el usuario autenticado.
     * Repetir la operación no crea duplicados ni falla.
     */
    @Transactional
    public void eliminar(Long notificacionId, HttpSession session) {
        LoginResponse usuario = authService.currentUser(session);
        try {
            Notificacion notificacion = notificacionRepository.findById(notificacionId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Notificación no encontrada con ID " + notificacionId));

            validarAcceso(notificacion, usuario);

            if (notificacionInactivaRepository.existsByNotificacionIdAndUsuarioId(
                    notificacionId,
                    usuario.getUserId()
            )) {
                log.warn(
                        "Notificación ya eliminada para el usuario: id={}, usuario={}",
                        notificacionId,
                        usuario.getUserId()
                );
                return;
            }

            NotificacionInactiva inactiva = new NotificacionInactiva();
            inactiva.setNotificacion(notificacion);
            inactiva.setUsuarioId(usuario.getUserId());
            notificacionInactivaRepository.save(inactiva);

            log.info(
                    "Notificación eliminada para el usuario: id={}, usuario={}",
                    notificacionId,
                    usuario.getUserId()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Error al eliminar notificación: id={}, usuario={}",
                    notificacionId,
                    usuario.getUserId(),
                    exception
            );
            throw exception;
        }
    }

    private void validarDatosDeCreacion(
            String rolDestinatario,
            DescriptorAudiencia audiencia,
            String titulo,
            String descripcion,
            LoginResponse remitente
    ) {
        if (rolDestinatario == null || rolDestinatario.isBlank()) {
            throw new IllegalArgumentException("El rol destinatario es obligatorio");
        }
        if (audiencia == null || audiencia.grupoDestinatario() == null
                || audiencia.grupoDestinatario().isBlank()) {
            throw new IllegalArgumentException("El grupo destinatario es obligatorio");
        }
        if (!audiencia.grupoDestinatario().startsWith("/")
                || audiencia.grupoDestinatario().endsWith("/")) {
            throw new IllegalArgumentException("El grupo destinatario no tiene un path válido");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
        if (remitente == null || remitente.getUserId() == null
                || remitente.getUserId().isBlank()) {
            throw new IllegalArgumentException("El remitente autenticado es obligatorio");
        }
    }

    private void validarAcceso(Notificacion notificacion, LoginResponse usuario) {
        if (!notificacion.getRolDestinatario().equals(usuario.getRole())
                || !perteneceAlGrupo(notificacion.getGrupoDestinatario(), usuario.getGroupPath())) {
            throw new AccesoDenegadoException(
                    "El usuario no puede eliminar esta notificación");
        }
    }

    private boolean perteneceAlGrupo(String grupoDestinatario, String grupoUsuario) {
        return grupoDestinatario.equals(grupoUsuario)
                || grupoUsuario.startsWith(grupoDestinatario + "/");
    }

    private NotificacionResponse toResponse(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTitulo(),
                notificacion.getDescripcion(),
                notificacion.getFechaCreacion(),
                notificacion.getRemitenteUsername(),
                new DescriptorAudiencia(notificacion.getGrupoDestinatario())
        );
    }
}
