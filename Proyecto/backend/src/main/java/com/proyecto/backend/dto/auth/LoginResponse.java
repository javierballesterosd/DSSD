package com.proyecto.backend.dto.auth;

import lombok.Getter;
import lombok.Setter;


//Representa los datos públicos que Spring Boot devuelve después de consultar Bonita:
@Getter
@Setter
public class LoginResponse {

    private long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String rol;

}
