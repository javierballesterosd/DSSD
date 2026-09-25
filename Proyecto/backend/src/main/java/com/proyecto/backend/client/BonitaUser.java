package com.proyecto.backend.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BonitaUser (
        String id,
        @JsonProperty("userName") String username,
        String firstname,
        String lastname
){}
