package com.optiroute.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.optiroute.backend.dto.request.PointOfInterestRequest;
import com.optiroute.backend.entity.event.PointOfInterest;
import com.optiroute.backend.repository.PointOfInterestRepository;

import jakarta.persistence.EntityNotFoundException;

class PointOfInterestServiceTest {

    private final PointOfInterestRepository repository = mock(PointOfInterestRepository.class);
    private final PointOfInterestService service = new PointOfInterestService(repository);
    private final PointOfInterestRequest request = new PointOfInterestRequest(
            " Updated label ", "New address", new BigDecimal("48.1234567"), new BigDecimal("2.1234567"));

    @Test
    void shouldUpdateExistingPointIncludingCoordinates() {
        PointOfInterest point = PointOfInterest.builder().id(1L).label("Old label").build();
        when(repository.findById(1L)).thenReturn(Optional.of(point));
        when(repository.save(point)).thenReturn(point);

        var response = service.update(1L, request);

        assertEquals(1L, response.id());
        assertEquals("Updated label", response.label());
        assertEquals(request.address(), response.address());
        assertEquals(request.latitude(), response.latitude());
        assertEquals(request.longitude(), response.longitude());
        verify(repository).save(point);
    }

    @Test
    void shouldAllowClearingAddress() {
        PointOfInterest point = PointOfInterest.builder().id(1L).address("Old address").build();
        when(repository.findById(1L)).thenReturn(Optional.of(point));
        when(repository.save(point)).thenReturn(point);

        var response = service.update(1L, new PointOfInterestRequest(
                request.label(), null, request.latitude(), request.longitude()));

        assertEquals(null, response.address());
    }

    @Test
    void shouldRejectUpdateOfUnknownPoint() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.update(1L, request));

        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldDeleteAndFlushToCheckReferences() {
        PointOfInterest point = PointOfInterest.builder().id(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(point));

        service.delete(1L);

        var order = inOrder(repository);
        order.verify(repository).findById(1L);
        order.verify(repository).delete(point);
        order.verify(repository).flush();
    }

    @Test
    void shouldPropagateReferenceConflict() {
        PointOfInterest point = PointOfInterest.builder().id(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(point));
        doThrow(new DataIntegrityViolationException("Referenced by event")).when(repository).flush();

        assertThrows(DataIntegrityViolationException.class, () -> service.delete(1L));
    }

    @Test
    void shouldRejectDeleteOfUnknownPoint() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.delete(1L));

        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldPreserveCreationAndListing() {
        when(repository.save(any(PointOfInterest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(request);
        assertEquals("Updated label", created.label());
        assertEquals(request.address(), created.address());
        assertEquals(request.latitude(), created.latitude());
        assertEquals(request.longitude(), created.longitude());

        PointOfInterest point = PointOfInterest.builder().id(1L).label("Existing point").build();
        when(repository.findAll()).thenReturn(List.of(point));
        assertEquals(1L, service.getAll().getFirst().id());
    }
}
