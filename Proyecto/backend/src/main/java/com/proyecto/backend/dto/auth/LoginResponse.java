package com.proyecto.backend.dto.auth;

import lombok.Getter;
import lombok.Setter;


//Representa los datos públicos que Spring Boot devuelve después de consultar Bonita:
@Getter
@Setter
public class LoginResponse {

    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String role;
    private String group;
    // Solo para el rol ONG; null en los demás roles
    private Long ongId;
    private String ongNombre;
    // Solo para el rol MUNICIPAL; null en los demás roles
    private Long municipioId;
    private String municipioNombre;
    // Para MUNICIPAL (región de su municipio) y COORDINADOR (su región); null en los demás roles
    private Long regionId;
    private String regionNombre;

}
