package com.proyecto.backend.repository;

import com.proyecto.backend.model.entity.Emergencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmergenciaRepository extends JpaRepository<Emergencia, Long> {
}