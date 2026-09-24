package com.proyecto.backend.model.entity;

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
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emergencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emergencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_gravedad", nullable = false)
    private NivelGravedad nivelGravedad;

    @Column(name = "zona_afectada", nullable = false, length = 200)
    private String zonaAfectada;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    /** Id del caso en Bonita (el proceso se inicia al registrar la emergencia). */
    @Column(name = "bonita_case_id", unique = true)
    private String bonitaCaseId;

    /** Ventana de recepción de ofertas: la convocatoria es por emergencia, no por lote. */
    @Column(name = "fecha_apertura_ofertas")
    private LocalDateTime fechaAperturaOfertas;

    @Column(name = "fecha_cierre_ofertas")
    private LocalDateTime fechaCierreOfertas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "municipio_id", nullable = false)
    private Municipio municipio;

    @OneToMany(mappedBy = "emergencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lote> lotes = new ArrayList<>();
}
