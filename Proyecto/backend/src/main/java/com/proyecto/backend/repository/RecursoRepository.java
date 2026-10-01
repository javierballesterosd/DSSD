package com.proyecto.backend.repository;

import com.proyecto.backend.model.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {
}