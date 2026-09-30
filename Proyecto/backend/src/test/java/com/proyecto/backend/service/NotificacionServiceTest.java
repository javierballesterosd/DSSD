package com.proyecto.backend.service;

import com.proyecto.backend.dto.auth.LoginResponse;
import com.proyecto.backend.dto.notificacion.DescriptorAudiencia;
import com.proyecto.backend.dto.notificacion.NotificacionResponseDTO;
import com.proyecto.backend.exception.AccesoDenegadoException;
import com.proyecto.backend.exception.RecursoNoEncontradoException;
import com.proyecto.backend.model.Notificacion;
import com.proyecto.backend.repository.NotificacionInactivaRepository;
import com.proyecto.backend.repository.NotificacionRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private NotificacionInactivaRepository notificacionInactivaRepository;
    @Mock
    private AuthService authService;
    @Mock
    private HttpSession session;

    private NotificacionService notificacionService;
    private LoginResponse coordinador;

    @BeforeEach
    void setUp() {
        notificacionService = new NotificacionService(
                notificacionRepository,
                notificacionInactivaRepository,
                authService
        );

        coordinador = usuario("coord-1", "COORDINADOR", "/Municipio/Region1");
    }

    @Test
    void creaNotificacionConElRemitenteYLaAudienciaIndicados() {
        Notificacion guardada = new Notificacion();
        guardada.setId(10L);
        guardada.setRolDestinatario("COORDINADOR");
        guardada.setGrupoDestinatario("/Municipio/Region1");
        guardada.setTitulo("Nueva emergencia");
        guardada.setDescripcion("Se registró una emergencia");
        guardada.setRemitenteUserId("coord-1");
        guardada.setRemitenteUsername("coordinador");
        guardada.setFechaCreacion(LocalDateTime.now());
        when(notificacionRepository.save(any(Notificacion.class))).thenReturn(guardada);

        NotificacionResponseDTO response = notificacionService.crear(
                "COORDINADOR",
                new DescriptorAudiencia("/Municipio/Region1"),
                "Nueva emergencia",
                "Se registró una emergencia",
                coordinador
        );

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitulo()).isEqualTo("Nueva emergencia");
        assertThat(response.getAudiencia().grupoDestinatario())
                .isEqualTo("/Municipio/Region1");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        assertThat(captor.getValue().getRemitenteUserId()).isEqualTo("coord-1");
        assertThat(captor.getValue().getGrupoDestinatario())
                .isEqualTo("/Municipio/Region1");
    }

    @Test
    void listaNotificacionesUsandoElRolGrupoYUsuarioDeLaSesion() {
        when(authService.currentUser(session)).thenReturn(coordinador);
        Notificacion notificacion = notificacion(10L, "COORDINADOR",
                "/Municipio/Region1", "Nueva emergencia");
        when(notificacionRepository.findVisiblesParaUsuario(
                "COORDINADOR",
                "/Municipio/Region1",
                "coord-1"
        )).thenReturn(List.of(notificacion));

        List<NotificacionResponseDTO> resultado =
                notificacionService.listarVisibles(session);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(10L);
        verify(notificacionRepository).findVisiblesParaUsuario(
                "COORDINADOR",
                "/Municipio/Region1",
                "coord-1"
        );
    }

    @Test
    void eliminarPermiteUnSubgrupoDelGrupoDestinatario() {
        when(authService.currentUser(session)).thenReturn(
                usuario("coord-1", "COORDINADOR",
                        "/Municipio/Region1/LaPlata/ZonaNorte")
        );
        Notificacion notificacion = notificacion(
                10L,
                "COORDINADOR",
                "/Municipio/Region1/LaPlata",
                "Nueva emergencia"
        );
        when(notificacionRepository.findById(10L)).thenReturn(Optional.of(notificacion));
        when(notificacionInactivaRepository.existsByNotificacionIdAndUsuarioId(
                10L, "coord-1"
        )).thenReturn(false);

        notificacionService.eliminar(10L, session);

        verify(notificacionInactivaRepository).save(any());
    }

    @Test
    void eliminarRechazaUnRolDistintoAunquePertenezcaAlGrupo() {
        when(authService.currentUser(session)).thenReturn(
                usuario("municipal-1", "MUNICIPAL", "/Municipio/Region1")
        );
        Notificacion notificacion = notificacion(
                10L,
                "COORDINADOR",
                "/Municipio/Region1",
                "Nueva emergencia"
        );
        when(notificacionRepository.findById(10L)).thenReturn(Optional.of(notificacion));

        assertThatThrownBy(() -> notificacionService.eliminar(10L, session))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(notificacionInactivaRepository, never()).save(any());
    }

    @Test
    void eliminarRechazaUnGrupoQueNoEsLaAudiencia() {
        when(authService.currentUser(session)).thenReturn(
                usuario("coord-2", "COORDINADOR", "/Municipio/Region10")
        );
        Notificacion notificacion = notificacion(
                10L,
                "COORDINADOR",
                "/Municipio/Region1",
                "Nueva emergencia"
        );
        when(notificacionRepository.findById(10L)).thenReturn(Optional.of(notificacion));

        assertThatThrownBy(() -> notificacionService.eliminar(10L, session))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(notificacionInactivaRepository, never()).save(any());
    }

    @Test
    void eliminarEsIdempotenteSiYaFueRealizadaPorElUsuario() {
        when(authService.currentUser(session)).thenReturn(coordinador);
        Notificacion notificacion = notificacion(
                10L,
                "COORDINADOR",
                "/Municipio/Region1",
                "Nueva emergencia"
        );
        when(notificacionRepository.findById(10L)).thenReturn(Optional.of(notificacion));
        when(notificacionInactivaRepository.existsByNotificacionIdAndUsuarioId(
                10L, "coord-1"
        )).thenReturn(true);

        notificacionService.eliminar(10L, session);

        verify(notificacionInactivaRepository, never()).save(any());
    }

    @Test
    void eliminarFallaSiLaNotificacionNoExiste() {
        when(authService.currentUser(session)).thenReturn(coordinador);
        when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.eliminar(99L, session))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(notificacionInactivaRepository, never()).save(any());
    }

    @Test
    void crearRechazaUnGrupoDestinatarioInvalido() {
        assertThatThrownBy(() -> notificacionService.crear(
                "COORDINADOR",
                new DescriptorAudiencia("Municipio/Region1"),
                "Título",
                "Descripción",
                coordinador
        )).isInstanceOf(IllegalArgumentException.class);

        verify(notificacionRepository, never()).save(any());
    }

    private LoginResponse usuario(String userId, String role, String groupPath) {
        LoginResponse usuario = new LoginResponse();
        usuario.setUserId(userId);
        usuario.setUsername(userId + ".username");
        usuario.setRole(role);
        usuario.setGroupPath(groupPath);
        return usuario;
    }

    private Notificacion notificacion(
            Long id,
            String rol,
            String grupo,
            String titulo
    ) {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(id);
        notificacion.setRolDestinatario(rol);
        notificacion.setGrupoDestinatario(grupo);
        notificacion.setTitulo(titulo);
        notificacion.setDescripcion("Descripción");
        notificacion.setRemitenteUserId("remitente-1");
        notificacion.setRemitenteUsername("remitente");
        notificacion.setFechaCreacion(LocalDateTime.now());
        return notificacion;
    }
}
