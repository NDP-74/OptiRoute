package com.optiroute.backend.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/transports");

    @Test
    void shouldExposePlanningConflictAsUnprocessableEntityWithItsExplanation() {
        var exception = new TransportPlanningException("Planning incompatible : départ avant minuit.");
        var response = handler.handleTransportPlanningException(exception, request);

        assertEquals(422, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("TRANSPORT_PLANNING_ERROR", response.getBody().code());
        assertEquals(exception.getMessage(), response.getBody().message());
    }

    @Test
    void shouldMapNotFoundToNotFound() {
        assertEquals(404, handler.handleNotFound(new EntityNotFoundException("Service not found"), request).getStatusCode().value());
        assertEquals(404, handler.handleNotFound(new NoSuchElementException(), request).getStatusCode().value());
    }

    @Test
    void shouldMapBusinessErrorsToBadRequestWithTheirMessage() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("Email already used"), request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Email already used", response.getBody().message());
    }

    @Test
    void shouldMapDataIntegrityViolationToConflictWithoutLeakingSqlDetails() {
        var response = handler.handleDataIntegrity(new DataIntegrityViolationException("fk_event_tractor violates ..."), request);

        assertEquals(409, response.getStatusCode().value());
        assertFalse(response.getBody().message().contains("fk_event_tractor"));
    }

    @Test
    void shouldMapRoutingProviderFailureToBadGatewayWithoutProviderDetails() {
        var response = handler.handleRoutingProvider(new RoutingProviderException("HERE a répondu 503 : maintenance"), request);

        assertEquals(502, response.getStatusCode().value());
        assertEquals("ROUTING_PROVIDER_ERROR", response.getBody().code());
        assertFalse(response.getBody().message().contains("HERE a répondu"));
    }

    @Test
    void shouldKeepResponseStatusReasonAndStatus() {
        var response = handler.handleResponseStatus(new ResourceInUseException("ce tracteur", 2), request);

        assertEquals(409, response.getStatusCode().value());
        assertTrue(response.getBody().message().contains("utilisé par 2 événements"));

        var unauthorized = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect."), request);
        assertEquals(401, unauthorized.getStatusCode().value());
    }

    @Test
    void shouldHideUnexpectedErrors() {
        var response = handler.handleUnexpectedException(new IllegalStateException("secret internal detail"), request);

        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().message().contains("secret"));
    }
}
