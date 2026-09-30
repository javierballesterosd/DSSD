package com.proyecto.backend.repository;

import com.proyecto.backend.model.ItemLote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemLoteRepository extends JpaRepository<ItemLote, Long> {

    List<ItemLote> findByLoteId(Long loteId);
}
