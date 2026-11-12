package com.proyecto.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "ong")
@Getter
@Setter
@NoArgsConstructor
public class Ong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador común a todos los sistemas (app local, Sistema Nacional, Bonita). Es estable y no
    // depende de ids autogenerados ni del path de Bonita. Lo asigna el alta si no viene (@PrePersist).
    @Column(name = "codigo", unique = true, length = 36)
    private String codigo;

    @PrePersist
    void asignarCodigo() {
        if (codigo == null) {
            codigo = UUID.randomUUID().toString();
        }
    }

    @Column(name = "razon_social", nullable = false, unique = true, length = 200)
    private String razonSocial;

    // Path del subgrupo de la ONG en la organización de Bonita (ej. /ONG/CruzRojaLaPlata).
    // Es la clave que vincula los usuarios de Bonita con esta fila; no depende de ids de Bonita.
    @Column(name = "bonita_group_path", nullable = false, unique = true, length = 200)
    private String bonitaGroupPath;

    // Vive dentro de un Set (Oferta.ongs): igualdad por id, hashCode constante.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ong)) {
            return false;
        }
        Ong other = (Ong) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Ong.class.hashCode();
    }
}
