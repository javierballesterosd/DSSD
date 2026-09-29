package com.proyecto.backend.repository;

import com.proyecto.backend.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    Optional<Lote> findFirstByEmergenciaIdOrderByIdDesc(Long emergenciaId);

}