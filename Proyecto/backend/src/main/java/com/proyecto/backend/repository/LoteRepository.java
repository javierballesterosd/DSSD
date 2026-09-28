package com.proyecto.backend.repository;

import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.Lote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    @EntityGraph(attributePaths = {"emergencia", "emergencia.municipio", "items", "items.recurso"})
    List<Lote> findByEstado(EstadoLote estado);

    @EntityGraph(attributePaths = {"emergencia", "emergencia.municipio", "items", "items.recurso"})
    Optional<Lote> findDetalleById(Long id);
}
