package com.optiroute.backend.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    @Test
    void shouldExposePlanningConflictAsUnprocessableEntityWithItsExplanation() {
        var exception = new TransportPlanningException("Planning incompatible : départ avant minuit.");
        var response = new GlobalExceptionHandler().handleTransportPlanningException(exception,
            new MockHttpServletRequest("POST", "/api/transports"));

        assertEquals(422, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("TRANSPORT_PLANNING_ERROR", response.getBody().code());
        assertEquals(exception.getMessage(), response.getBody().message());
    }
}
