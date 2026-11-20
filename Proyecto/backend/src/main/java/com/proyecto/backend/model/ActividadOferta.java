package com.proyecto.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Avance de una oferta adjudicada en una actividad de la emergencia. Las actividades las define el municipio
 * para toda la emergencia; cada oferta adjudicada debe completarlas todas. Las ONGs de un consorcio comparten
 * la fila: si una la marca, queda hecha para las demás.
 */
@Entity
@Table(
        name = "actividad_oferta",
        uniqueConstraints = @UniqueConstraint(columnNames = {"actividad_id", "oferta_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class ActividadOferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oferta_id", nullable = false)
    private Oferta oferta;

    @Column(nullable = false)
    private boolean finalizada;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    /** Username de Bonita de quien la marcó como hecha. */
    @Column(name = "usuario_finalizacion", length = 150)
    private String usuarioFinalizacion;

    /** ONG del consorcio desde la que se marcó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ong_finalizacion_id")
    private Ong ongFinalizacion;
}
