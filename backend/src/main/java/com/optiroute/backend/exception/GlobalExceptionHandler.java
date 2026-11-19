package com.optiroute.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.NoSuchElementException;

import com.optiroute.backend.dto.response.ApiErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception, HttpServletRequest request) {
        return error(exception.getStatusCode(), "REQUEST_ERROR", exception.getReason(), request, exception);
    }

    @ExceptionHandler(TransportPlanningException.class)
    public ResponseEntity<ApiErrorResponse> handleTransportPlanningException(TransportPlanningException exception,
            HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "TRANSPORT_PLANNING_ERROR", exception.getMessage(), request, exception);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiErrorResponse> handleValidationException(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Les données envoyées sont invalides.", request, exception);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "La requête est mal formée ou incomplète.", request, exception);
    }

    // Erreurs métier levées par les services (email déjà utilisé, véhicule hors trajet, etc.)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), request, exception);
    }

    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "La ressource demandée est introuvable.", request, exception);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Cette adresse n'existe pas.", request, exception);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Méthode HTTP non autorisée.", request, exception);
    }

    // Filet de sécurité : contrainte SQL violée (élément encore référencé, doublon, etc.)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Contrainte de données violée sur {} {} : {}", request.getMethod(), request.getRequestURI(),
            exception.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT",
            "L'opération est impossible : les données sont encore utilisées ou en doublon.", request, null);
    }

    @ExceptionHandler(RoutingProviderException.class)
    public ResponseEntity<ApiErrorResponse> handleRoutingProvider(RoutingProviderException exception, HttpServletRequest request) {
        log.error("Fournisseur d'itinéraire en erreur sur {} {} : {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return error(HttpStatus.BAD_GATEWAY, "ROUTING_PROVIDER_ERROR",
            "Le service de calcul d'itinéraire est indisponible. Réessayez dans quelques instants.", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("Erreur inattendue sur {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Une erreur interne est survenue.", request, null);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatusCode status, String code, String message, HttpServletRequest request,
            Exception exception) {
        if (exception != null) {
            log.debug("{} {} -> {} {} : {}", request.getMethod(), request.getRequestURI(), status.value(), code, exception.getMessage());
        }
        return ResponseEntity.status(status).body(new ApiErrorResponse(status.value(), code, message, request.getRequestURI(), Instant.now()));
    }
}
