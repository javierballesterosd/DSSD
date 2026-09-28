package com.proyecto.backend.repository;

import com.proyecto.backend.model.Oferta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    /** Ofertas de un lote en las que participa la ONG (sola o en consorcio), la más nueva primero. */
    @EntityGraph(attributePaths = {
            "ongs", "detalles", "detalles.ong", "detalles.itemLote", "detalles.itemLote.recurso",
            "lote", "lote.emergencia"
    })
    @Query("select o from Oferta o join o.ongs g "
            + "where o.lote.id = :loteId and g.id = :ongId order by o.fechaOferta desc")
    List<Oferta> findByLoteIdAndOngId(@Param("loteId") Long loteId, @Param("ongId") Long ongId);
}
