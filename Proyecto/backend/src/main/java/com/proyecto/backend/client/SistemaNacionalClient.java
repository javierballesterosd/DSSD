package com.proyecto.backend.client;

import com.proyecto.backend.exception.SistemaNacionalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.proyecto.backend.dto.OfertasConsolidadasResponse;
import org.springframework.web.client.RestClientResponseException;

@Component
public class SistemaNacionalClient {
    private final RestClient client;
    private final String username;
    private final String password;

    public SistemaNacionalClient(RestClient.Builder builder,
            @Value("${nacional.base-url}") String baseUrl,
            @Value("${nacional.username}") String username,
            @Value("${nacional.password}") String password) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.client = builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
        this.username = username;
        this.password = password;
    }

    public record Reserva(Long emergenciaId, Long loteId, Long ofertaId) {}
    public record ItemReserva(Long ongId, Long recursoId, Integer cantidad, String estado) {}
    public record Reservas(List<ItemReserva> items) {}
    public record Login(String token) {}

    public void enviarOfertas(String token, OfertasConsolidadasResponse ofertas) {
        try {
            client.post().uri("/ofertas").headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON).body(ofertas).retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            throw new SistemaNacionalException("No se pudieron enviar las ofertas al SN", ex);
        }
    }

    public Optional<Reservas> reservarTemporal(String token, Reserva reserva) {
        try {
            Reservas respuesta = client.post().uri("/reservas/temporal").headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON).body(reserva).retrieve().body(Reservas.class);
            if (respuesta == null || respuesta.items() == null || respuesta.items().isEmpty()) {
                throw new IllegalStateException("El SN no devolvió los recursos reservados");
            }
            return Optional.of(respuesta);
        } catch (RestClientResponseException ex) {
            // La API nacional rechaza la oferta completa si no puede reservar sus recursos.
            if (ex.getStatusCode().value() == 409) return Optional.empty();
            throw new SistemaNacionalException("No se pudo reservar temporalmente la oferta", ex);
        } catch (RuntimeException ex) {
            throw new SistemaNacionalException("No se pudo reservar temporalmente la oferta", ex);
        }
    }

    public String autenticar() {
        try {
            if (password.isBlank()) throw new IllegalStateException("Falta configurar NACIONAL_PASSWORD");
            Login login = client.post().uri("/login").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", username, "password", password)).retrieve().body(Login.class);
            if (login == null || login.token() == null || login.token().isBlank()) {
                throw new IllegalStateException("El SN no devolvió un token");
            }
            return login.token();
        } catch (RuntimeException ex) {
            throw new SistemaNacionalException("No se pudo autenticar con SN", ex);
        }
    }

    public Reservas consultar(String token, Reserva reserva) {
        try {
            Reservas respuesta = client.get().uri(builder -> builder.path("/reservas")
                    .queryParam("emergenciaId", reserva.emergenciaId())
                    .queryParam("loteId", reserva.loteId()).queryParam("ofertaId", reserva.ofertaId()).build())
                    .headers(h -> h.setBearerAuth(token)).retrieve().body(Reservas.class);
            if (respuesta == null || respuesta.items() == null) throw new IllegalStateException("Respuesta vacía de SN");
            return respuesta;
        } catch (RuntimeException ex) {
            throw new SistemaNacionalException("No se pudieron consultar las reservas", ex);
        }
    }

    public void decidir(String token, Reserva reserva, boolean aceptar) {
        try {
            client.post().uri(aceptar ? "/reservas/definitiva" : "/reservas/liberacion")
                    .headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                    .body(reserva).retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            throw new SistemaNacionalException("No se pudo sincronizar la oferta " + reserva.ofertaId(), ex);
        }
    }
}
