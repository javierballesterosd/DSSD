package com.proyecto.backend.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notificacion_inactiva",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notificacion_usuario",
                columnNames = {"notificacion_id", "usuario_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class NotificacionInactiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notificacion_id", nullable = false)
    private Notificacion notificacion;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Column(name = "fecha_eliminacion", nullable = false)
    private LocalDateTime fechaEliminacion;

    @PrePersist
    void establecerFechaEliminacion() {
        if  (this.fechaEliminacion == null) {
            this.fechaEliminacion = LocalDateTime.now();
        }
    }
}
