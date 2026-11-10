package com.proyecto.backend.repository;

import com.proyecto.backend.model.EstadoLote;
import com.proyecto.backend.model.Lote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select l from Lote l where l.emergencia.id = :id "
            + "and l.id = (select max(ultimo.id) from Lote ultimo where ultimo.emergencia.id = :id)")
    Optional<Lote> bloquearUltimoDeEmergencia(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select l from Lote l where l.id = :id")
    Optional<Lote> bloquearPorId(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<Lote> findFirstByEmergenciaIdOrderByIdDesc(Long emergenciaId);

    Optional<Lote> findFirstByEmergenciaIdAndEstado(Long emergenciaId, EstadoLote estado);

    @EntityGraph(attributePaths = {"emergencia", "emergencia.municipio", "items", "items.recurso"})
    List<Lote> findByEstado(EstadoLote estado);

    /** Lotes (en cualquier estado) de las emergencias de los municipios de una región. */
    @EntityGraph(attributePaths = {"emergencia", "emergencia.municipio", "items", "items.recurso"})
    List<Lote> findByEmergenciaMunicipioRegionIdOrderByFechaCreacionDesc(Long regionId);

    /**
     * Regla de negocio: una emergencia tiene un solo lote no cancelado. La usa la creación de lotes
     * (LoteService.crear, rama feature/publicacion-lotes) y la respalda un índice único parcial en la base.
     */
    boolean existsByEmergenciaIdAndEstadoNot(Long emergenciaId, EstadoLote estado);

    @EntityGraph(attributePaths = {"emergencia", "emergencia.municipio", "items", "items.recurso"})
    Optional<Lote> findDetalleById(Long id);
}
