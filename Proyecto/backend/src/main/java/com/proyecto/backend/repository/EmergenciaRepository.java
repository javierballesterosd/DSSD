package com.proyecto.backend.repository;

import com.proyecto.backend.model.Emergencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface EmergenciaRepository extends JpaRepository<Emergencia, Long> {

    @Query("""
        SELECT e
        FROM Emergencia e
        LEFT JOIN e.lotes l
        WHERE l.id IS NULL
           OR l.id = (
               SELECT MAX(l2.id)
               FROM Lote l2
               WHERE l2.emergencia.id = e.id
           )
        ORDER BY
            CASE
                WHEN l.id IS NULL THEN 0
                ELSE 1
            END ASC,
            e.fechaRegistro DESC
        """)
    Page<Emergencia> findEmergenciasParaLotes(Pageable pageable);
}