package com.proyecto.backend.repository;

import com.proyecto.backend.dto.DashboardRankingResponse;
import com.proyecto.backend.model.NivelGravedad;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.sql.Timestamp;

@Repository
@RequiredArgsConstructor
public class DashboardRepository {

    private final JdbcTemplate jdbc;

    public List<DashboardRankingResponse> ongsPorOfertas(LocalDate desde, LocalDate hasta) {
        var parametros = new ArrayList<Object>();
        String periodo = periodo("o.fecha_oferta", desde, hasta, parametros);
        // Una oferta de consorcio cuenta una vez para cada ONG participante.
        return consultar("""
                SELECT g.id, g.razon_social AS nombre, COUNT(DISTINCT o.id) AS total,
                       CAST(NULL AS varchar) AS unidad_medida
                FROM ong g
                LEFT JOIN oferta_ong og ON og.ong_id = g.id
                LEFT JOIN oferta o ON o.id = og.oferta_id AND o.estado <> 'ELIMINADA' %s
                GROUP BY g.id, g.razon_social
                ORDER BY total DESC, nombre ASC, g.id ASC
                """.formatted(periodo), parametros.toArray());
    }

    public List<DashboardRankingResponse> ongsPorRecursosAsignados(LocalDate desde, LocalDate hasta) {
        var parametros = new ArrayList<Object>();
        String periodo = periodo("l.fecha_decision_municipal", desde, hasta, parametros);
        // Agrupar primero evita multiplicar las cantidades por los miembros del consorcio.
        return consultar("""
                SELECT g.id, g.razon_social AS nombre, COALESCE(a.total, 0) AS total,
                       CAST(NULL AS varchar) AS unidad_medida
                FROM ong g
                LEFT JOIN (
                    SELECT d.ong_id, SUM(d.cantidad_aceptada) AS total
                    FROM detalle_oferta d JOIN oferta o ON o.id = d.oferta_id
                    JOIN lote l ON l.id = o.lote_id
                    WHERE o.estado IN ('ADJUDICADA', 'EN_EJECUCION', 'FINALIZADA') %s
                    GROUP BY d.ong_id
                ) a ON a.ong_id = g.id
                ORDER BY total DESC, nombre ASC, g.id ASC
                """.formatted(periodo), parametros.toArray());
    }

    public List<DashboardRankingResponse> municipiosPorEmergencias(LocalDate desde, LocalDate hasta) {
        var parametros = new ArrayList<Object>();
        String periodo = periodo("e.fecha_registro", desde, hasta, parametros);
        return consultar("""
                SELECT m.id, m.nombre, COUNT(e.id) AS total,
                       CAST(NULL AS varchar) AS unidad_medida
                FROM municipio m LEFT JOIN emergencia e ON e.municipio_id = m.id %s
                GROUP BY m.id, m.nombre
                ORDER BY total DESC, nombre ASC, m.id ASC
                """.formatted(periodo), parametros.toArray());
    }

    public List<DashboardRankingResponse> recursosPorNecesidad(LocalDate desde, LocalDate hasta) {
        var parametros = new ArrayList<Object>();
        String periodo = periodo("l.fecha_creacion", desde, hasta, parametros);
        return consultar("""
                SELECT r.id, r.nombre, COALESCE(n.total, 0) AS total, r.unidad_medida
                FROM recurso r
                LEFT JOIN (
                    SELECT i.recurso_id, SUM(i.cantidad_requerida) AS total
                    FROM item_lote i JOIN lote l ON l.id = i.lote_id
                    WHERE l.estado <> 'CANCELADO' %s
                    GROUP BY i.recurso_id
                ) n ON n.recurso_id = r.id
                ORDER BY total DESC, r.nombre ASC, r.id ASC
                """.formatted(periodo), parametros.toArray());
    }

    public List<DashboardRankingResponse> recursosPorCantidad() {
        return consultar("""
                SELECT r.id, r.nombre, COALESCE(SUM(i.cantidad_disponible), 0) AS total,
                       r.unidad_medida
                FROM recurso r LEFT JOIN inventario_ong i ON i.recurso_id = r.id
                GROUP BY r.id, r.nombre, r.unidad_medida
                ORDER BY total DESC, r.nombre ASC, r.id ASC
                """);
    }

    /** Fechas inclusivas: hasta se traduce al inicio del dia siguiente, sin truncar horas. */
    private String periodo(String columna, LocalDate desde, LocalDate hasta, List<Object> parametros) {
        String filtro = "";
        if (desde != null) {
            filtro += " AND " + columna + " >= ?";
            parametros.add(Timestamp.valueOf(desde.atStartOfDay()));
        }
        if (hasta != null) {
            filtro += " AND " + columna + " < ?";
            parametros.add(Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
        }
        return filtro;
    }

    public List<com.proyecto.backend.dto.DashboardBonitaResponse.EmergenciaLocal> emergenciasConCaso(
            Long municipioId, NivelGravedad gravedad, Long recursoId, Long ongId) {
        String sql = """
                SELECT e.id, e.bonita_case_id, m.nombre, e.descripcion
                FROM emergencia e JOIN municipio m ON m.id = e.municipio_id
                WHERE e.bonita_case_id IS NOT NULL
                """;
        var parametros = new ArrayList<Object>();
        if (municipioId != null) {
            sql += " AND e.municipio_id = ?";
            parametros.add(municipioId);
        }
        if (gravedad != null) {
            sql += " AND e.nivel_gravedad = ?";
            parametros.add(gravedad.name());
        }
        // EXISTS evita contar una emergencia varias veces por sus lotes, ofertas o consorcios.
        if (ongId != null) {
            sql += """
                     AND EXISTS (
                        SELECT 1 FROM lote l
                        JOIN oferta o ON o.lote_id = l.id
                        JOIN oferta_ong og ON og.oferta_id = o.id
                        WHERE l.emergencia_id = e.id AND og.ong_id = ?
                          AND o.estado <> 'ELIMINADA'
                    """;
            parametros.add(ongId);
            if (recursoId != null) {
                // Los dos filtros deben coincidir en un aporte de la misma ONG.
                sql += """
                         AND EXISTS (
                            SELECT 1 FROM detalle_oferta d
                            JOIN item_lote il ON il.id = d.item_lote_id
                            WHERE d.oferta_id = o.id AND d.ong_id = og.ong_id
                              AND d.cantidad_ofrecida > 0 AND il.recurso_id = ?)
                        """;
                parametros.add(recursoId);
            }
            sql += ")";
        } else if (recursoId != null) {
            sql += """
                     AND EXISTS (
                        SELECT 1 FROM lote l JOIN item_lote il ON il.lote_id = l.id
                        WHERE l.emergencia_id = e.id AND il.recurso_id = ?)
                    """;
            parametros.add(recursoId);
        }
        return jdbc.query(sql, (rs, row) -> new com.proyecto.backend.dto.DashboardBonitaResponse.EmergenciaLocal(
                rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), parametros.toArray());
    }

    private List<DashboardRankingResponse> consultar(String sql, Object... parametros) {
        return jdbc.query(sql, (rs, row) -> new DashboardRankingResponse(
                rs.getLong("id"), rs.getString("nombre"), rs.getLong("total"),
                rs.getString("unidad_medida")), parametros);
    }
}
