package com.proyecto.backend.exception;

public class BonitaIntegrationException extends RuntimeException {

    public BonitaIntegrationException(String message) {
        super(message);
    }

    public BonitaIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
