package com.proyecto.backend.exception;

public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException() {
        super("No hay una sesión autenticada");
    }
}
