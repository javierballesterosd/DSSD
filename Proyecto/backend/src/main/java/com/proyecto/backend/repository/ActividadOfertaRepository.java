package com.proyecto.backend.repository;

import com.proyecto.backend.model.ActividadOferta;
import com.proyecto.backend.model.EstadoOferta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActividadOfertaRepository extends JpaRepository<ActividadOferta, Long> {

    boolean existsByActividadEmergenciaId(Long emergenciaId);

    boolean existsByActividadEmergenciaIdAndFinalizadaFalse(Long emergenciaId);

    @Query("select count(distinct a.actividad.id) from ActividadOferta a where a.actividad.emergencia.id = :id")
    long contarActividades(@Param("id") Long emergenciaId);

    @Query("select count(distinct a.actividad.id) from ActividadOferta a "
            + "where a.actividad.emergencia.id = :id and a.finalizada = false")
    long contarActividadesPendientes(@Param("id") Long emergenciaId);

    Optional<ActividadOferta> findByActividadIdAndOfertaId(Long actividadId, Long ofertaId);

    /** Avance de las ofertas en las que participa la ONG, agrupable por oferta. */
    @EntityGraph(attributePaths = {
            "actividad", "ongFinalizacion", "oferta", "oferta.lote", "oferta.lote.emergencia",
            "oferta.lote.emergencia.municipio"
    })
    @Query("select a from ActividadOferta a join a.oferta o join o.ongs g "
            + "where g.id = :ongId and o.estado in :estados order by o.id, a.actividad.id")
    List<ActividadOferta> findDeOng(@Param("ongId") Long ongId, @Param("estados") Collection<EstadoOferta> estados);
}
