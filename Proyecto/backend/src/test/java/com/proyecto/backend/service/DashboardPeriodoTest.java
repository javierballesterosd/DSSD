package com.proyecto.backend.service;

import com.proyecto.backend.repository.DashboardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DashboardPeriodoTest {
    @Test void aplicaDiasCompletosConParametrosEnLosCuatroRankingsHistoricos() {
        var jdbc = mock(JdbcTemplate.class);
        var repository = new DashboardRepository(jdbc);
        var desde = LocalDate.of(2026, 10, 1);
        var hasta = LocalDate.of(2026, 10, 31);
        repository.ongsPorOfertas(desde, hasta);
        repository.ongsPorRecursosAsignados(desde, hasta);
        repository.municipiosPorEmergencias(desde, hasta);
        repository.recursosPorNecesidad(desde, hasta);
        assertThat(mockingDetails(jdbc).getInvocations()).hasSize(4).allSatisfy(call -> {
            assertThat((String) call.getRawArguments()[0]).contains(" >= ?", " < ?");
            assertThat((Object[]) call.getRawArguments()[2]).containsExactly(
                    Timestamp.valueOf("2026-10-01 00:00:00"), Timestamp.valueOf("2026-11-01 00:00:00"));
        });
    }

    @Test void admiteIntervalosAbiertosYSinFiltro() {
        var jdbc = mock(JdbcTemplate.class);
        var repository = new DashboardRepository(jdbc);
        var fecha = LocalDate.of(2026, 10, 1);
        repository.municipiosPorEmergencias(fecha, null);
        repository.municipiosPorEmergencias(null, fecha);
        repository.municipiosPorEmergencias(null, null);
        var calls = mockingDetails(jdbc).getInvocations().stream().toList();
        assertThat((Object[]) calls.get(0).getRawArguments()[2]).containsExactly(Timestamp.valueOf("2026-10-01 00:00:00"));
        assertThat((Object[]) calls.get(1).getRawArguments()[2]).containsExactly(Timestamp.valueOf("2026-10-02 00:00:00"));
        assertThat((Object[]) calls.get(2).getRawArguments()[2]).isEmpty();
    }
    @Test void municipioYGravedadSeFiltranConParametrosSobreLasEmergenciasLocales() {
        var jdbc = mock(JdbcTemplate.class);
        var repository = new DashboardRepository(jdbc);
        repository.emergenciasConCaso(2L, com.proyecto.backend.model.NivelGravedad.ALTA, null, null);
        var call = mockingDetails(jdbc).getInvocations().iterator().next();
        assertThat((String) call.getRawArguments()[0]).contains("e.municipio_id = ?", "e.nivel_gravedad = ?");
        assertThat((Object[]) call.getRawArguments()[2]).containsExactly(2L, "ALTA");
    }
    @Test void ongYRecursoDebenCoincidirEnElAporteYNoDuplicanEmergenciasPorConsorcio() {
        var jdbc = mock(JdbcTemplate.class);
        var repository = new DashboardRepository(jdbc);
        repository.emergenciasConCaso(null, null, 3L, 4L);
        var call = mockingDetails(jdbc).getInvocations().iterator().next();
        assertThat((String) call.getRawArguments()[0]).contains("AND EXISTS", "og.ong_id = ?",
                "d.oferta_id = o.id", "d.ong_id = og.ong_id", "il.recurso_id = ?", "o.estado <> 'ELIMINADA'");
        assertThat((Object[]) call.getRawArguments()[2]).containsExactly(4L, 3L);
    }

    @Test void recursoSoloFiltraLasNecesidadesDelLoteSinExigirOfertas() {
        var jdbc = mock(JdbcTemplate.class);
        var repository = new DashboardRepository(jdbc);
        repository.emergenciasConCaso(null, null, 3L, null);
        var call = mockingDetails(jdbc).getInvocations().iterator().next();
        assertThat((String) call.getRawArguments()[0]).contains("il.lote_id = l.id", "il.recurso_id = ?")
                .doesNotContain("JOIN oferta o");
        assertThat((Object[]) call.getRawArguments()[2]).containsExactly(3L);
    }
}
