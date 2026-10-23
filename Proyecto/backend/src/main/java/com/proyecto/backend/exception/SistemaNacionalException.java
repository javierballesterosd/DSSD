package com.proyecto.backend.exception;

public class SistemaNacionalException extends RuntimeException {
    public SistemaNacionalException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
