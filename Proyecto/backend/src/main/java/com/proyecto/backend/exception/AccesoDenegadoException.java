package com.proyecto.backend.exception;

/** El usuario está autenticado pero su rol no le permite la operación. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
