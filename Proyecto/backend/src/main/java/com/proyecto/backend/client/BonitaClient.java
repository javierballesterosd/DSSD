package com.proyecto.backend.client;

import com.proyecto.backend.exception.BonitaIntegrationException;
import com.proyecto.backend.exception.InvalidCredentialsException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class BonitaClient {

    private final RestClient restClient;

    public BonitaClient(RestClient bonitaRestClient) {
        this.restClient = bonitaRestClient;
    }

    private static final int TAREA_REINTENTOS = 10;
    private static final long TAREA_ESPERA_MS = 300;

    @Value("${bonita.process.name:Proceso 1}")
    private String processName;

    public BonitaSession login(String username, String password){
        //Bonita espera los datos del login en formato formulario, no en JSON
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);
        form.add("redirect", "false");

        return restClient.post().uri("/loginservice")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange((request, response) -> {
                   if (response.getStatusCode().value() == 401) {
                       throw new InvalidCredentialsException();
                   }

                   if (!response.getStatusCode().is2xxSuccessful()) {
                       throw new BonitaIntegrationException(
                               "Bonita rechazó el login con status "
                                       + response.getStatusCode().value()
                       );
                   }

                   var cookies = response.getHeaders().getValuesAsList("Set-Cookie");

                   String jsessionId = extraerCookie(cookies, "JSESSIONID");
                   String apiToken = extraerCookie(cookies, "X-Bonita-API-Token");

                   if (jsessionId == null || apiToken == null) {
                       throw new BonitaIntegrationException(
                               "Bonita no devolvió las cookies esperadas"
                       );
                   }

                   return new BonitaSession(jsessionId, apiToken);
                });
    }

    public BonitaSessionInfo getCurrentSession(BonitaSession session){
        return restClient.get()
                .uri("/API/system/session/unusedid")
                .header("Cookie", cookieHeader(session))
                .retrieve()
                .body(BonitaSessionInfo.class);
    }

    public BonitaUser getUser(BonitaSession session, String userId){
        return restClient.get()
                .uri("/API/identity/user/{userId}", userId)
                .header("Cookie", cookieHeader(session))
                .retrieve()
                .body(BonitaUser.class);
    }

    //Metodo encargado de obtener rol y grupo de los usuarios
    public List<BonitaMembership> getMemberships(BonitaSession session, String userId){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/API/identity/membership")
                        .queryParam("p", "0")
                        .queryParam("c", "10")
                        .queryParam("f", "user_id=" + userId)
                        .queryParam("d", "role_id")
                        .queryParam("d", "group_id")
                        .build())
                .header("Cookie", cookieHeader(session))
                .retrieve()
                .body(new ParameterizedTypeReference<List<BonitaMembership>>() {});
    }


    public String buscarProcesoId(BonitaSession session) {
        return buscarProcesoId(session, processName);
    }

    /** Id del proceso habilitado con el nombre configurado (bonita.process.name). */
    public String buscarProcesoId(BonitaSession session, String nombreProceso) {
        List<Map<String, Object>> procesos = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/API/bpm/process")
                        .queryParam("p", "0")
                        .queryParam("c", "1")
                        .queryParam("o", "version DESC")
                        .queryParam("f", "name=" + nombreProceso)
                        .queryParam("f", "activationState=ENABLED")
                        .build())
                .headers(h -> aplicarSesion(h, session))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        if (procesos == null || procesos.isEmpty()) {
            throw new BonitaIntegrationException(
                    "No hay un proceso habilitado con el nombre '" + nombreProceso + "' en Bonita");
        }

        return String.valueOf(procesos.get(0).get("id"));
    }

    /** Inicia un caso del proceso (sin contrato de inicio) y devuelve su id. */
    public String iniciarCaso(BonitaSession session, String processId) {
        Map<?, ?> respuesta = restClient.post()
                .uri("/API/bpm/process/{id}/instantiation", processId)
                .headers(h -> aplicarSesion(h, session))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .body(Map.class);
        if (respuesta == null || respuesta.get("caseId") == null) {
            throw new BonitaIntegrationException("Bonita no devolvió el id del caso creado");
        }
        return respuesta.get("caseId").toString();
    }

    /**
     * Id de la tarea humana pendiente del caso con ese nombre. La tarea se crea en forma asíncrona
     * después de iniciar el caso, así que se reintenta unas veces.
     */
    public String buscarTareaPendiente(BonitaSession session, String caseId, String nombreTarea) {
        for (int intento = 0; intento < TAREA_REINTENTOS; intento++) {
            List<Map<String, Object>> tareas = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/API/bpm/humanTask")
                            .queryParam("p", "0")
                            .queryParam("c", "1")
                            .queryParam("f", "caseId=" + caseId)
                            .queryParam("f", "name=" + nombreTarea)
                            .build())
                    .headers(h -> aplicarSesion(h, session))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (tareas != null && !tareas.isEmpty()) {
                return String.valueOf(tareas.get(0).get("id"));
            }
            try {
                Thread.sleep(TAREA_ESPERA_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new BonitaIntegrationException(
                "La tarea '" + nombreTarea + "' no apareció en el caso " + caseId);
    }

    /** Asigna la tarea al usuario de la sesión y la completa con los datos del contrato. */
    public void ejecutarTarea(BonitaSession session, String taskId, Map<String, Object> contrato) {
        restClient.post()
                .uri("/API/bpm/userTask/{id}/execution?assign=true", taskId)
                .headers(h -> aplicarSesion(h, session))
                .contentType(MediaType.APPLICATION_JSON)
                .body(contrato)
                .retrieve()
                .toBodilessEntity();
    }

    // Bonita exige la cookie de sesión y el token anti-CSRF en cada llamada
    private void aplicarSesion(HttpHeaders headers, BonitaSession session) {
        headers.set("Cookie", cookieHeader(session));
        headers.set("X-Bonita-API-Token", session.apiToken());
    }

    private String cookieHeader(BonitaSession session){
        return "JSESSIONID=" + session.jsessionId()
                + "; X-Bonita-API-Token=" + session.apiToken();
    }

    private String extraerCookie(java.util.List<String> cookies, String nombre) {
        return cookies.stream()
                .filter(cookie -> cookie.startsWith(nombre + "="))
                .map(cookie -> cookie.substring(
                        nombre.length() + 1,
                        cookie.indexOf(';')
                ))
                .findFirst()
                .orElse(null);
    }
}
