package com.proyecto.backend.repository;

import com.proyecto.backend.model.Ong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OngRepository extends JpaRepository<Ong, Long> {

    List<Ong> findAllByOrderByRazonSocialAsc();
}
