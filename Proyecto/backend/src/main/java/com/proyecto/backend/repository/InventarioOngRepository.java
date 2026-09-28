package com.proyecto.backend.repository;

import com.proyecto.backend.model.InventarioOng;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface InventarioOngRepository extends JpaRepository<InventarioOng, Long> {

    @Query("select i from InventarioOng i join fetch i.ong join fetch i.recurso where i.ong.id in :ongIds")
    List<InventarioOng> findByOngIdIn(Collection<Long> ongIds);
}
