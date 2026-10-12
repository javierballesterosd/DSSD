package com.proyecto.backend.repository;

import com.proyecto.backend.model.InventarioOng;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventarioOngRepository extends JpaRepository<InventarioOng, Long> {
 
    @Query("select i from InventarioOng i join fetch i.ong join fetch i.recurso where i.ong.id in :ongIds")
    List<InventarioOng> findByOngIdIn(Collection<Long> ongIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventarioOng i WHERE i.ong.id = :ongId AND i.recurso.id = :recursoId")
    Optional<InventarioOng> findByOngIdAndRecursoIdForUpdate(@Param("ongId") Long ongId, @Param("recursoId") Long recursoId);

    Optional<InventarioOng> findByOngIdAndRecursoId(Long ongId, Long recursoId);
}
