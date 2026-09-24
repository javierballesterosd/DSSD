package com.proyecto.backend.client;

import com.proyecto.backend.exception.BonitaIntegrationException;
import com.proyecto.backend.exception.InvalidCredentialsException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class BonitaClient {

    private final RestClient restClient;

    public BonitaClient(RestClient bonitaRestClient) {
        this.restClient = bonitaRestClient;
    }

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
