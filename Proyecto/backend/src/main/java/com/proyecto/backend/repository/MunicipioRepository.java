package com.proyecto.backend.repository;

import com.proyecto.backend.model.Municipio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    @EntityGraph(attributePaths = "region")
    Optional<Municipio> findByBonitaGroupPath(String bonitaGroupPath);
}
