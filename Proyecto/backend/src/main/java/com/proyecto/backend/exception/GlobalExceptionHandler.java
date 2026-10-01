package com.proyecto.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(),
                "RecursoNoEncontradoException", ex.getMessage());
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(),
                "ReglaNegocioException", ex.getMessage());
        return construir(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException ex, HttpServletRequest request) {
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(),
                "UnauthenticatedException", ex.getMessage());
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), List.of());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccesoDenegadoException ex, HttpServletRequest request) {
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(),
                "AccesoDenegadoException", ex.getMessage());
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(),
                "InvalidCredentialsException", ex.getMessage());
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), List.of());
    }

    @ExceptionHandler(BonitaIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleBonitaIntegration(BonitaIntegrationException ex, HttpServletRequest request) {
        log.error("{} {} -> BonitaIntegrationException: {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex);
        // El detalle técnico (etapa, status, ids de Bonita) queda solo en el log
        return construir(HttpStatus.BAD_GATEWAY,
                "No se pudo completar la operación en Bonita. Intentá de nuevo más tarde.", List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        log.warn("{} {} -> Datos inválidos: {}", request.getMethod(), request.getRequestURI(), detalles);
        return construir(HttpStatus.BAD_REQUEST, "Datos inválidos", detalles);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("{} {} -> Ruta inexistente", request.getMethod(), request.getRequestURI());
        return construir(HttpStatus.NOT_FOUND, "Recurso no encontrado", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerica(Exception ex, HttpServletRequest request) {
        log.error("{} {} -> Error no controlado: {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje, List<String> detalles) {
        ErrorResponse cuerpo = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                detalles
        );
        return ResponseEntity.status(status).body(cuerpo);
    }
}
