package com.proyecto.backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Foto inmutable de una oferta tras un alta, una edición o una baja. */
@Entity
@Table(
        name = "oferta_version",
        uniqueConstraints = @UniqueConstraint(columnNames = {"oferta_id", "numero"})
)
@Getter
@Setter
@NoArgsConstructor
public class OfertaVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oferta_id", nullable = false)
    private Oferta oferta;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cambio", nullable = false)
    private TipoCambioOferta tipoCambio;

    /** Estado de la oferta en el momento del cambio. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOferta estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Username de Bonita de quien hizo el cambio. */
    @Column(nullable = false)
    private String usuario;

    /** ONG del usuario que hizo el cambio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ong_id", nullable = false)
    private Ong ong;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleOfertaVersion> detalles = new ArrayList<>();
}
