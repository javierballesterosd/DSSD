package com.proyecto.backend.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BonitaGroup(
        String id,
        String name,
        String displayName,
        @JsonProperty("parent_path") String parentPath
) {

    // Path completo del grupo (ej. /ONG/CruzRojaLaPlata). Estable entre máquinas, a diferencia del id.
    public String path() {
        String padre = parentPath == null ? "" : parentPath;
        return padre + "/" + name;
    }
}
