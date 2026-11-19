package com.optiroute.backend.service.vehicle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.optiroute.backend.exception.ResourceInUseException;
import com.optiroute.backend.repository.EventRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.repository.vehicle.TrailerTypeRepository;

class VehicleDeletionTest {

    private final TractorRepository tractors = mock(TractorRepository.class);
    private final SemiTrailerRepository semiTrailers = mock(SemiTrailerRepository.class);
    private final EventRepository events = mock(EventRepository.class);
    private final TractorService tractorService = new TractorService(tractors, events);
    private final SemiTrailerService semiTrailerService = new SemiTrailerService(semiTrailers, mock(TrailerTypeRepository.class), events);

    @Test
    void shouldRefuseToDeleteVehiclesUsedByEvents() {
        when(tractors.existsById(1L)).thenReturn(true);
        when(semiTrailers.existsById(2L)).thenReturn(true);
        when(events.countByTractorId(1L)).thenReturn(1L);
        when(events.countBySemiTrailerId(2L)).thenReturn(3L);

        var tractorError = assertThrows(ResourceInUseException.class, () -> tractorService.deleteTractor(1L));
        var semiTrailerError = assertThrows(ResourceInUseException.class, () -> semiTrailerService.deleteSemiTrailer(2L));

        assertEquals(409, tractorError.getStatusCode().value());
        assertTrue(tractorError.getReason().contains("utilisé par 1 événement."));
        assertTrue(semiTrailerError.getReason().contains("utilisé par 3 événements"));
        verify(tractors, never()).deleteById(anyLong());
        verify(semiTrailers, never()).deleteById(anyLong());
    }

    @Test
    void shouldDeleteUnusedVehicles() {
        when(tractors.existsById(1L)).thenReturn(true);
        when(semiTrailers.existsById(2L)).thenReturn(true);

        tractorService.deleteTractor(1L);
        semiTrailerService.deleteSemiTrailer(2L);

        verify(tractors).deleteById(1L);
        verify(semiTrailers).deleteById(2L);
    }
}
