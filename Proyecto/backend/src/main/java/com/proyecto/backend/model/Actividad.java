package com.proyecto.backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "actividad")
@Getter
@Setter
@NoArgsConstructor
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(nullable = true, length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "emergencia_id", nullable = false)
    private Emergencia emergencia;

    /** Una fila por cada oferta adjudicada de la emergencia que debe completar esta actividad. */
    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActividadOferta> asignaciones = new ArrayList<>();

    /** Derivado: hecha cuando todas las ofertas adjudicadas la completaron (falsa mientras no haya ninguna). */
    public boolean isFinalizada() {
        return !asignaciones.isEmpty() && asignaciones.stream().allMatch(ActividadOferta::isFinalizada);
    }
}
