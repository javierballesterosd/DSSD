package com.proyecto.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Rol de aplicación normalizado, por ejemplo MUNICIPAL, ONG o COORDINADOR. */
    @Column(name = "rol_destinatario", nullable = false, length = 50)
    private String rolDestinatario;

    /**
     * Path del grupo padre de Bonita que recibe la notificación.
     * Ejemplos: /Municipio/Region1, /Municipio/Region1/LaPlata,
     * /ONG/CaritasBuenosAires.
     */
    @Column(name = "grupo_destinatario", nullable = false, length = 255)
    private String grupoDestinatario;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    /**
     * Identificador del usuario en Bonita.
     */
    @Column(name = "remitente_user_id", nullable = false, length = 100)
    private String remitenteUserId;

    /**
     * Snapshot para auditoría y visualización; la identidad sigue siendo remitenteUserId.
     */
    @Column(name = "remitente_username", length = 150)
    private String remitenteUsername;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "notificacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificacionInactiva> descartes = new ArrayList<>();

    @PrePersist
    void establecerFechaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
