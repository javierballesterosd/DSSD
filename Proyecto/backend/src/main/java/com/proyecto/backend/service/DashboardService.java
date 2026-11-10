package com.proyecto.backend.service;

import com.proyecto.backend.client.BonitaClient;
import com.proyecto.backend.client.BonitaSession;
import com.proyecto.backend.dto.DashboardBonitaResponse.*;
import com.proyecto.backend.dto.DashboardRankingResponse;
import com.proyecto.backend.exception.BonitaIntegrationException;
import com.proyecto.backend.exception.ReglaNegocioException;
import com.proyecto.backend.repository.DashboardRepository;
import com.proyecto.backend.model.NivelGravedad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    private final BonitaClient bonita;

    @Transactional(readOnly = true)
    public List<DashboardRankingResponse> ongsPorOfertas(Integer limite, LocalDate desde, LocalDate hasta) {
        validarLimite(limite);
        validarFechas(desde, hasta);
        return limitar(dashboardRepository.ongsPorOfertas(desde, hasta), limite);
    }

    @Transactional(readOnly = true)
    public List<DashboardRankingResponse> ongsPorRecursosAsignados(Integer limite, LocalDate desde, LocalDate hasta) {
        validarLimite(limite);
        validarFechas(desde, hasta);
        return limitar(dashboardRepository.ongsPorRecursosAsignados(desde, hasta), limite);
    }

    @Transactional(readOnly = true)
    public List<DashboardRankingResponse> municipiosPorEmergencias(Integer limite, LocalDate desde, LocalDate hasta) {
        validarLimite(limite);
        validarFechas(desde, hasta);
        return limitar(dashboardRepository.municipiosPorEmergencias(desde, hasta), limite);
    }

    @Transactional(readOnly = true)
    public List<DashboardRankingResponse> recursosPorNecesidad(Integer limite, LocalDate desde, LocalDate hasta) {
        validarLimite(limite);
        validarFechas(desde, hasta);
        return limitar(dashboardRepository.recursosPorNecesidad(desde, hasta), limite);
    }

    @Transactional(readOnly = true)
    public List<DashboardRankingResponse> recursosPorCantidad(Integer limite) {
        validarLimite(limite);
        return limitar(dashboardRepository.recursosPorCantidad(), limite);
    }

    private void validarFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha desde no puede ser posterior a hasta");
        }
        if (hasta != null && hasta.equals(LocalDate.MAX)) {
            throw new ReglaNegocioException("La fecha hasta esta fuera del rango permitido");
        }
    }

    private void validarLimite(Integer limite) {
        if (limite != null && limite < 1) {
            throw new ReglaNegocioException("El limite debe ser mayor que cero");
        }
    }

    private List<DashboardRankingResponse> limitar(List<DashboardRankingResponse> ranking, Integer limite) {
        return limite == null ? ranking : ranking.stream().limit(limite).toList();
    }

    private static final String VALIDACION = "Validar en el Sistema Nacional (POST Cloud)";

    private static final DateTimeFormatter FECHA = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd().toFormatter();

    private record Ejecucion(String caso, String nombre, LocalDateTime inicio, LocalDateTime fin) {}
    /** Rango aplicado a la finalización de cada ejecución; incluye ambos días. */

    public List<Tiempo> etapas(BonitaSession session, LocalDate desde, LocalDate hasta, Integer limite,
                               Long municipioId, NivelGravedad gravedad, Long recursoId, Long ongId) {
        validar(desde, hasta, limite, municipioId, recursoId, ongId);
        var grupos = ejecuciones(session, municipioId, gravedad, recursoId, ongId).stream().filter(e -> enRango(e.fin(), desde, hasta))
                .collect(Collectors.groupingBy(Ejecucion::nombre));
        var resultado = grupos.entrySet().stream().map(e -> promedio(e.getKey(), e.getValue().stream()
                        .map(x -> segundos(x.inicio(), x.fin())).toList()))
                .sorted(Comparator.comparing(Tiempo::promedioSegundos).reversed().thenComparing(Tiempo::etapa));
        return (limite == null ? resultado : resultado.limit(limite)).toList();
    }

    public List<RankingEtapa> rankingEtapas(BonitaSession session, LocalDate desde, LocalDate hasta,
                                            Integer limite, Long municipioId, NivelGravedad gravedad, Long recursoId, Long ongId) {
        var etapas = etapas(session, desde, hasta, limite, municipioId, gravedad, recursoId, ongId);
        return java.util.stream.IntStream.range(0, etapas.size()).mapToObj(indice -> {
            var etapa = etapas.get(indice);
            return new RankingEtapa(indice + 1, etapa.etapa(), etapa.cantidad(), etapa.promedioSegundos());
        }).toList();
    }

    public Tiempo tarea(BonitaSession session, String nombre, LocalDate desde, LocalDate hasta,
                          Long municipioId, NivelGravedad gravedad, Long recursoId, Long ongId) {
        validar(desde, hasta, null, municipioId, recursoId, ongId);
        return promedio(nombre, ejecuciones(session, municipioId, gravedad, recursoId, ongId).stream().filter(e -> e.nombre().equals(nombre))
                .filter(e -> enRango(e.fin(), desde, hasta)).map(e -> segundos(e.inicio(), e.fin())).toList());
    }
    /** Una muestra por emergencia: inicio del caso hasta su primera validación completada. */

    public Tiempo validacion(BonitaSession session, LocalDate desde, LocalDate hasta,
                          Long municipioId, NivelGravedad gravedad, Long recursoId, Long ongId) {
        validar(desde, hasta, null, municipioId, recursoId, ongId);
        Map<String, LocalDateTime> primeras = new HashMap<>();
        for (var e : ejecuciones(session, municipioId, gravedad, recursoId, ongId)) {
            if (e.nombre().equals(VALIDACION)) primeras.merge(e.caso(), e.fin(),
                    (a, b) -> a.isBefore(b) ? a : b);
        }
        var casos = casos(session);
        var tiempos = new ArrayList<Double>();
        for (var entry : primeras.entrySet()) {
            var caso = casos.get(entry.getKey());
            if (caso == null || !enRango(entry.getValue(), desde, hasta)) continue;
            var inicio = fecha(caso.get("start"));
            if (inicio != null && !entry.getValue().isBefore(inicio)) tiempos.add(segundos(inicio, entry.getValue()));
        }
        return promedio("Hasta la primera validacion nacional", tiempos);
    }
    /** Solo casos realmente completados; se excluyen los cancelados y abortados.
     * Requiere implementar el cierre del proceso para generar muestras reales del flujo actual.
     */

    public Tiempo finalizadas(BonitaSession session, LocalDate desde, LocalDate hasta,
                          Long municipioId, NivelGravedad gravedad, Long recursoId, Long ongId) {
        validar(desde, hasta, null, municipioId, recursoId, ongId);
        var ids = locales(municipioId, gravedad, recursoId, ongId);
        var tiempos = new ArrayList<Double>();
        var vistos = new HashSet<String>();
        for (var c : bonita.datosDashboard(session, "archivedCase")) {
            String id = texto(c, "sourceObjectId");
            if (!ids.containsKey(id) || !(texto(c, "state").equals("completed") || texto(c, "state").equals("6"))) continue;
            var inicio = fecha(c.get("start"));
            var fin = fecha(c.get("end_date"));
            if (inicio != null && fin != null && !fin.isBefore(inicio) && enRango(fin, desde, hasta) && vistos.add(id)) {
                tiempos.add(segundos(inicio, fin));
            }
        }
        return promedio("Emergencias finalizadas", tiempos);
    }
    /** Identifica casos detenidos en la tarea modelada; no implica que su monitoreo esté implementado.
     * El rango corresponde al inicio del caso, no a una fotografía histórica.
     */

    public List<Seguimiento> seguimiento(BonitaSession session, LocalDate desde, LocalDate hasta,
                          Long municipioId, NivelGravedad gravedad, Long recursoId, Long ongId) {
        validar(desde, hasta, null, municipioId, recursoId, ongId);
        var locales = locales(municipioId, gravedad, recursoId, ongId);
        var casos = bonita.datosDashboard(session, "case").stream().collect(Collectors.toMap(
                c -> texto(c, "id"), c -> c, (a, b) -> b));
        return bonita.datosDashboard(session, "activity").stream()
                .filter(a -> texto(a, "name").strip().equals("Monitorear despliegue"))
                .filter(a -> !Set.of("completed", "cancelled", "aborted", "skipped").contains(texto(a, "state")))
                .filter(a -> locales.containsKey(texto(a, "caseId")) && casos.containsKey(texto(a, "caseId")))
                .filter(a -> enRango(fecha(casos.get(texto(a, "caseId")).get("start")), desde, hasta))
                .map(a -> {
                    var e = locales.get(texto(a, "caseId"));
                    return new Seguimiento(e.emergenciaId(), e.caseId(), e.municipio(), e.descripcion(),
                            texto(a, "name"), texto(a, "state"));
                }).distinct().sorted(Comparator.comparing(Seguimiento::emergenciaId)).toList();
    }

    private Map<String, EmergenciaLocal> locales(Long municipioId, NivelGravedad gravedad, Long recursoId, Long ongId) {
        return dashboardRepository.emergenciasConCaso(municipioId, gravedad, recursoId, ongId).stream().collect(Collectors.toMap(EmergenciaLocal::caseId, e -> e));
    }

    private Map<String, Map<String, Object>> casos(BonitaSession session) {
        var casos = new HashMap<String, Map<String, Object>>();
        bonita.datosDashboard(session, "archivedCase").forEach(c -> casos.put(texto(c, "sourceObjectId"), c));
        bonita.datosDashboard(session, "case").forEach(c -> casos.put(texto(c, "id"), c));
        return casos;
    }

    private List<Ejecucion> ejecuciones(BonitaSession session, Long municipioId, NivelGravedad gravedad, Long recursoId, Long ongId) {
        var locales = locales(municipioId, gravedad, recursoId, ongId);
        // sourceObjectId identifica la ejecución, incluso cuando se repite una tarea en un bucle.
        var grupos = bonita.datosDashboard(session, "archivedFlowNode").stream()
                .filter(n -> locales.containsKey(texto(n, "caseId")))
                .filter(n -> Set.of("USER_TASK", "AUTOMATIC_TASK", "MANUAL_TASK").contains(texto(n, "type")))
                .collect(Collectors.groupingBy(n -> texto(n, "caseId") + ":" + requerido(n, "sourceObjectId")));
        var resultado = new ArrayList<Ejecucion>();
        for (var estados : grupos.values()) {
            var terminado = estados.stream().filter(n -> texto(n, "state").equals("completed"))
                    .max(Comparator.comparing(n -> momento(n)));
            if (terminado.isEmpty()) continue;
            // Bonita archiva cada transición. El primer estado es el inicio de esa ejecución.
            var inicio = estados.stream().filter(n -> !texto(n, "state").equals("completed"))
                    .map(this::momento).min(Comparator.naturalOrder());
            if (inicio.isEmpty()) continue; // Sin historial suficiente no se inventa una duración.
            var n = terminado.get();
            var fin = momento(n);
            if (!fin.isBefore(inicio.get())) resultado.add(new Ejecucion(texto(n, "caseId"),
                    texto(n, "name").strip(), inicio.get(), fin));
        }
        return resultado;
    }

    private LocalDateTime momento(Map<String, Object> n) {
        var valor = n.get("reached_state_date");
        var fecha = fecha(valor == null || valor.toString().isBlank() ? n.get("archivedDate") : valor);
        if (fecha == null) throw new BonitaIntegrationException("Historial de Bonita sin fecha de transicion");
        return fecha;
    }

    private LocalDateTime fecha(Object valor) {
        if (valor == null || valor.toString().isBlank()) return null;
        try {
            return LocalDateTime.parse(valor.toString(), FECHA);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new BonitaIntegrationException("Formato de fecha inesperado en Bonita", ex);
        }
    }

    private String texto(Map<String, Object> fila, String campo) {
        return Objects.toString(fila.get(campo), "");
    }

    private String requerido(Map<String, Object> fila, String campo) {
        String valor = texto(fila, campo);
        if (valor.isBlank()) throw new BonitaIntegrationException("Historial de Bonita sin " + campo);
        return valor;
    }

    private boolean enRango(LocalDateTime fecha, LocalDate desde, LocalDate hasta) {
        return fecha != null && (desde == null || !fecha.toLocalDate().isBefore(desde))
                && (hasta == null || !fecha.toLocalDate().isAfter(hasta));
    }

    private double segundos(LocalDateTime inicio, LocalDateTime fin) {
        return Duration.between(inicio, fin).toMillis() / 1000.0;
    }

    private Tiempo promedio(String nombre, List<Double> tiempos) {
        return new Tiempo(nombre, tiempos.size(), tiempos.isEmpty() ? null :
                tiempos.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
    }

    private void validar(LocalDate desde, LocalDate hasta, Integer limite, Long municipioId, Long recursoId, Long ongId) {
        validarFechas(desde, hasta);
        validarLimite(limite);
        validarIdentificador(municipioId, "municipioId");
        validarIdentificador(recursoId, "recursoId");
        validarIdentificador(ongId, "ongId");
    }

    private void validarIdentificador(Long id, String nombre) {
        if (id != null && id < 1) {
            throw new ReglaNegocioException("El " + nombre + " debe ser mayor que cero");
        }
    }
}
