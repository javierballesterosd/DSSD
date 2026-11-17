package com.proyecto.backend.repository;

import com.proyecto.backend.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    boolean existsByEmergenciaIdAndFinalizadaFalse(Long emergenciaId);
}
