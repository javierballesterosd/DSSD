package com.proyecto.backend.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BonitaSessionInfo (
        @JsonProperty("user_id") String userId
) {}
