package com.proyecto.backend.repository;

import com.proyecto.backend.model.Oferta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    /** Ofertas vigentes (no eliminadas) de un lote en las que participa la ONG, la más nueva primero. */
    // "ongs" queda afuera a propósito: traerla junto con "detalles" (una List, o sea un bag) arma un
    // producto cartesiano y cada detalle aparece repetido una vez por ONG. Se carga lazy en la transacción.
    @EntityGraph(attributePaths = {
            "detalles", "detalles.ong", "detalles.itemLote", "detalles.itemLote.recurso",
            "lote", "lote.emergencia"
    })
    @Query("select o from Oferta o join o.ongs g "
            + "where o.lote.id = :loteId and g.id = :ongId "
            + "and o.estado <> com.proyecto.backend.model.EstadoOferta.ELIMINADA "
            + "order by o.fechaOferta desc")
    List<Oferta> findByLoteIdAndOngId(@Param("loteId") Long loteId, @Param("ongId") Long ongId);

    /** Ids de los lotes donde la ONG tiene al menos una oferta vigente (no eliminada). */
    @Query("select distinct o.lote.id from Oferta o join o.ongs g "
            + "where g.id = :ongId and o.estado <> com.proyecto.backend.model.EstadoOferta.ELIMINADA")
    List<Long> findLoteIdsConOfertaDeOng(@Param("ongId") Long ongId);

    /** Todas las ofertas (incluidas las eliminadas) para auditoría, opcionalmente de un solo lote. */
    @EntityGraph(attributePaths = {
            "detalles", "detalles.ong", "detalles.itemLote", "detalles.itemLote.recurso",
            "lote", "lote.emergencia"
    })
    @Query("select o from Oferta o where (:loteId is null or o.lote.id = :loteId) order by o.fechaOferta desc")
    List<Oferta> findAllParaAuditoria(@Param("loteId") Long loteId);
}
