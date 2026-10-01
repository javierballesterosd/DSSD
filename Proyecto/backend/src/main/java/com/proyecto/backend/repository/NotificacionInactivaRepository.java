package com.proyecto.backend.repository;

import com.proyecto.backend.model.NotificacionInactiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionInactivaRepository extends JpaRepository<NotificacionInactiva, Long> {

    boolean existsByNotificacionIdAndUsuarioId(Long notificacionId, String usuarioId);
}
