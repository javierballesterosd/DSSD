package com.proyecto.backend.repository;

import com.proyecto.backend.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Query("""
            SELECT n
            FROM Notificacion n
            WHERE n.rolDestinatario = :rolDestinatario
              AND (
                  :grupoUsuario = n.grupoDestinatario
                  OR :grupoUsuario LIKE CONCAT(n.grupoDestinatario, '/%')
              )
              AND NOT EXISTS (
                  SELECT ni.id
                  FROM NotificacionInactiva ni
                  WHERE ni.notificacion = n
                    AND ni.usuarioId = :usuarioId
              )
            ORDER BY n.fechaCreacion DESC
            """)
    List<Notificacion> findVisiblesParaUsuario(
            @Param("rolDestinatario") String rolDestinatario,
            @Param("grupoUsuario") String grupoUsuario,
            @Param("usuarioId") String usuarioId
    );
}
