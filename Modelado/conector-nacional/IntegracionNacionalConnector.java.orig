package com.rescuesync.bonita;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Se ejecuta exclusivamente en el motor Bonita. No se instala en el backend local. */
public class IntegracionNacionalConnector extends AbstractConnector {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Properties config = new Properties();
    private String backend;
    private String nacional;
    private String localToken;
    private String jwt;

    @Override public void validateInputParameters() {}

    @Override protected void executeBusinessLogic() throws ConnectorException {
        try {
            String operacion = (String) getInputParameter("operacion");
            Boolean aceptar = (Boolean) getInputParameter("aceptar");
            if ("RECHAZAR".equals(operacion) && Boolean.TRUE.equals(aceptar)) return;
            String envPath = (String) getInputParameter("RUTA_ENV");
            if (envPath == null || envPath.isBlank()) {
                throw new IllegalStateException("Configura RUTA_ENV con la ruta completa al .env en los parametros de Bonita");
            }
            Path env = Path.of(envPath.trim());
            if (!Files.isRegularFile(env)) throw new IllegalStateException("No se encontro el .env indicado en RUTA_ENV");
            try (InputStream in = Files.newInputStream(env)) { config.load(in); }
            backend = config.getProperty("BONITA_BACKEND_URL", "http://localhost:"
                    + config.getProperty("BACKEND_PORT", "8081")).replaceAll("/+$", "");
            nacional = requerido("NACIONAL_BASE_URL").replaceAll("/+$", "");
            localToken = requerido("BONITA_OFFERS_API_TOKEN");
            long emergencia = ((Number) getInputParameter("emergenciaId")).longValue();
            long lote = ((Number) getInputParameter("loteId")).longValue();
            String caso = Long.toString(getExecutionContext().getProcessInstanceId());
            String ruta = backend + "/api/emergencias/" + emergencia;
            Map<String,Object> plan = objeto(request("GET", ruta + "/integracion-nacional?loteId=" + lote
                    + "&caseId=" + caso, localToken, null, Set.of(200)).body());
            if (Boolean.TRUE.equals(plan.get("confirmada"))) return;
            boolean validar = "VALIDAR".equals(operacion);
            if (validar ? plan.get("decision") != null : !Objects.equals(plan.get("decision"), aceptar)) {
                throw new IllegalStateException("La fase o la decisión de Bonita no coincide con la intención local");
            }
            List<Map<String,Object>> ofertas = lista(plan.get("ofertas"));
            if (!ofertas.isEmpty()) {
                Map<String,Object> login = objeto(request("POST", nacional + "/login", null,
                        Map.of("username", requerido("NACIONAL_USERNAME"), "password", requerido("NACIONAL_PASSWORD")), Set.of(200)).body());
                jwt = (String) login.get("token");
                if (jwt == null || jwt.isBlank()) throw new IllegalStateException("El SN no devolvió JWT");
            }
            if (validar) {
                for (Map<String,Object> oferta : ofertas) {
                    Map<String,Object> ids = ids(emergencia, lote, oferta);
                    // Si se perdió la respuesta local, reutilizar la reserva nacional vigente.
                    var anterior = request("GET", nacional + "/reservas?emergenciaId=" + emergencia
                            + "&loteId=" + lote + "&ofertaId=" + oferta.get("ofertaId"), jwt, null, Set.of(200,404));
                    if (anterior.statusCode() == 200 && !activos(objeto(anterior.body()), Set.of("COMPROMETIDA")).isEmpty()) {
                        throw new IllegalStateException("La oferta ya tiene recursos comprometidos; revisar el estado del proceso");
                    }
                    List<Map<String,Object>> items = anterior.statusCode() == 200
                            ? activos(objeto(anterior.body()), Set.of("BLOQUEADA")) : List.of();
                    boolean aprobada = true;
                    if (items.isEmpty()) {
                        var reserva = request("POST", nacional + "/reservas/temporal", jwt, ids, Set.of(200,201,409));
                        aprobada = reserva.statusCode() != 409;
                        items = aprobada ? activos(objeto(reserva.body()), Set.of("BLOQUEADA")) : List.of();
                    }
                    if (aprobada && items.isEmpty()) throw new IllegalStateException("Reserva nacional vacía");
                    Map<String,Object> resultado = resultado(oferta, aprobada, items);
                    request("POST", ruta + "/validacion-sn", localToken,
                            respuesta(lote, caso, List.of(resultado)), Set.of(204));
                }
            } else {
                // Verificar TODO el conjunto antes de comprometer la primera oferta.
                if (Boolean.TRUE.equals(aceptar)) {
                    for (Map<String,Object> oferta : ofertas) {
                        Map<String,Object> reserva = objeto(request("GET", nacional + "/reservas?emergenciaId=" + emergencia
                                + "&loteId=" + lote + "&ofertaId=" + oferta.get("ofertaId"), jwt, null, Set.of(200)).body());
                        if (!cantidades(lista(oferta.get("items"))).equals(cantidades(activos(reserva,
                                Set.of("BLOQUEADA", "EXPIRADA", "COMPROMETIDA"))))) {
                            throw new IllegalStateException("Las reservas nacionales no coinciden con el conjunto local");
                        }
                    }
                }
                List<Map<String,Object>> resultados = new ArrayList<>();
                for (Map<String,Object> oferta : ofertas) {
                    Map<String,Object> reserva = objeto(request("POST", nacional
                            + (Boolean.TRUE.equals(aceptar) ? "/reservas/definitiva" : "/reservas/liberacion"),
                            jwt, ids(emergencia, lote, oferta), Set.of(200)).body());
                    List<Map<String,Object>> items = Boolean.TRUE.equals(aceptar)
                            ? activos(reserva, Set.of("COMPROMETIDA")) : lista(reserva.get("items"));
                    resultados.add(resultado(oferta, true, items));
                }
                request("POST", ruta + "/decision-municipal/confirmacion", localToken,
                        respuesta(lote, caso, resultados), Set.of(204));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectorException("Integración nacional interrumpida; reintentar el conector", e);
        } catch (Exception e) {
            throw new ConnectorException("No se completó la integración nacional: " + e.getMessage(), e);
        }
    }

    private String requerido(String nombre) {
        String valor = config.getProperty(nombre);
        if (valor == null || valor.isBlank()) throw new IllegalStateException("Falta " + nombre);
        return valor.trim();
    }
    private HttpResponse<String> request(String method, String url, String token, Object body, Set<Integer> esperados)
            throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(45))
                .header("Accept", "application/json");
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body == null) req.method(method, HttpRequest.BodyPublishers.noBody());
        else req.header("Content-Type", "application/json").method(method,
                HttpRequest.BodyPublishers.ofString(JsonOutput.toJson(body)));
        HttpResponse<String> res = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        if (!esperados.contains(res.statusCode())) {
            // No exponer cuerpos de respuesta, credenciales ni JWT en los logs del motor.
            throw new IllegalStateException(method + " " + URI.create(url).getPath() + " respondió HTTP " + res.statusCode());
        }
        return res;
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> objeto(String json) {
        return (Map<String,Object>) new JsonSlurper().parseText(json);
    }
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> lista(Object value) {
        if (!(value instanceof List)) throw new IllegalStateException("Respuesta sin lista de recursos u ofertas");
        return (List<Map<String,Object>>) value;
    }
    private static List<Map<String,Object>> activos(Map<String,Object> respuesta, Set<String> estados) {
        return lista(respuesta.get("items")).stream().filter(i -> estados.contains(i.get("estado"))).toList();
    }
    private static Map<String,Long> cantidades(List<Map<String,Object>> items) {
        Map<String,Long> resultado = new HashMap<>();
        for (var item : items) {
            long cantidad = ((Number) item.get("cantidad")).longValue();
            if (cantidad <= 0) throw new IllegalStateException("Cantidad nacional inválida");
            resultado.merge(item.get("ongId") + ":" + item.get("recursoId"), cantidad, Math::addExact);
        }
        return resultado;
    }
    private static Map<String,Object> ids(long emergencia, long lote, Map<String,Object> oferta) {
        return Map.of("emergenciaId", emergencia, "loteId", lote, "ofertaId", oferta.get("ofertaId"));
    }
    private static Map<String,Object> resultado(Map<String,Object> oferta, boolean aprobada, List<Map<String,Object>> items) {
        List<Map<String,Object>> recibos = items.stream().map(i -> Map.<String,Object>of(
                "ongId", i.get("ongId"), "recursoId", i.get("recursoId"),
                "cantidad", i.get("cantidad"), "estado", i.get("estado"))).toList();
        return Map.of("ofertaId", oferta.get("ofertaId"), "aprobada", aprobada, "items", recibos);
    }
    private static Map<String,Object> respuesta(long lote, String caso, List<Map<String,Object>> resultados) {
        return Map.of("loteId", lote, "caseId", caso, "resultados", resultados);
    }
}
