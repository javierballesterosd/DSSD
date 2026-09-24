package com.proyecto.backend.client;

import com.fasterxml.jackson.annotation.JsonProperty;

//Este record almacena la información del rol y grupo de un usuario
public record BonitaMembership(
        @JsonProperty("user_id") String userId,
        @JsonProperty("role_id") BonitaRole role,
        @JsonProperty("group_id") BonitaGroup group
) {
}
