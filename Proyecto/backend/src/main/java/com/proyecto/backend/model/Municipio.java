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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "municipio")
@Getter
@Setter
@NoArgsConstructor
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    // Path del subgrupo del municipio en la organización de Bonita (ej. /Municipio/Region1/LaPlata).
    // Es la clave que vincula los usuarios de Bonita con esta fila; no depende de ids de Bonita.
    @Column(name = "bonita_group_path", nullable = false, unique = true, length = 200)
    private String bonitaGroupPath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;
}
