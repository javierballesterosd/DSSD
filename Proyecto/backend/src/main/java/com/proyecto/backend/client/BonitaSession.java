package com.proyecto.backend.client;

//Bonita devuelve un JSESSIONID y un apiToken después de iniciar sesión. Esta clase representa esos datos.
public record BonitaSession(
        String jsessionId,
        String apiToken
) {
}