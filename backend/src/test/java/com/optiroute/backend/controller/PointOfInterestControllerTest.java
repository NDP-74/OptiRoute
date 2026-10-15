package com.optiroute.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.optiroute.backend.dto.request.PointOfInterestRequest;
import com.optiroute.backend.dto.response.PointOfInterestResponse;
import com.optiroute.backend.exception.GlobalExceptionHandler;
import com.optiroute.backend.service.PointOfInterestService;

import jakarta.persistence.EntityNotFoundException;

class PointOfInterestControllerTest {

    private final PointOfInterestService service = mock(PointOfInterestService.class);
    private MockMvc mvc;
    private static final String PAYLOAD = """
            {"label":"Updated point","address":"New address","latitude":48.5,"longitude":2.5}
            """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PointOfInterestController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void shouldUpdatePoint() throws Exception {
        when(service.update(eq(1L), any(PointOfInterestRequest.class))).thenReturn(
                new PointOfInterestResponse(1L, "Updated point", "New address",
                        new BigDecimal("48.5"), new BigDecimal("2.5"), null, null));

        mvc.perform(put("/api/points-of-interest/1").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.label").value("Updated point"))
                .andExpect(jsonPath("$.address").value("New address"))
                .andExpect(jsonPath("$.latitude").value(48.5))
                .andExpect(jsonPath("$.longitude").value(2.5));
    }

    @Test
    void shouldValidateUpdates() throws Exception {
        for (String invalidPayload : new String[] {
                PAYLOAD.replace("Updated point", " "),
                PAYLOAD.replace("Updated point", "x".repeat(256)),
                PAYLOAD.replace("New address", "x".repeat(501)),
                PAYLOAD.replace("48.5", "91"),
                PAYLOAD.replace("2.5", "-181"),
                PAYLOAD.replace("48.5", "null")
        }) {
            mvc.perform(put("/api/points-of-interest/1").contentType(MediaType.APPLICATION_JSON).content(invalidPayload))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnNotFoundForUnknownPoint() throws Exception {
        when(service.update(eq(1L), any(PointOfInterestRequest.class)))
                .thenThrow(new EntityNotFoundException());
        doThrow(new EntityNotFoundException()).when(service).delete(1L);

        mvc.perform(put("/api/points-of-interest/1").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/points-of-interest/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeletePoint() throws Exception {
        mvc.perform(delete("/api/points-of-interest/1"))
                .andExpect(status().isNoContent());
        verify(service).delete(1L);
    }

    @Test
    void shouldExplainReferenceConflict() throws Exception {
        doThrow(new DataIntegrityViolationException("Referenced by event")).when(service).delete(1L);

        mvc.perform(delete("/api/points-of-interest/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POINT_OF_INTEREST_IN_USE"));
    }
}
