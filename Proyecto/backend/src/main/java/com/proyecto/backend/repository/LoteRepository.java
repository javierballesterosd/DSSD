package com.proyecto.backend.repository;

import com.proyecto.backend.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByEmergenciaId(Long emergenciaId);
}