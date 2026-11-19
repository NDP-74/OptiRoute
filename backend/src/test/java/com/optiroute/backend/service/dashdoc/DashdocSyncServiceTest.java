package com.optiroute.backend.service.dashdoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.optiroute.backend.client.DashdocClient;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.ServiceRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.service.transport.TransportFacadeService;

class DashdocSyncServiceTest {

    @Test
    void acceptsASevenDayPeriod() {
        DashdocClient client = mock(DashdocClient.class);
        when(client.getTransports()).thenReturn(List.of());
        DashdocSyncService subject = service(client);

        var result = subject.synchronize(
            LocalDate.parse("2026-11-20"),
            LocalDate.parse("2026-11-26"));

        assertEquals(0, result.created());
        assertEquals(0, result.skipped());
    }

    @Test
    void rejectsPeriodsLongerThanSevenDaysBeforeCallingDashdoc() {
        DashdocClient client = mock(DashdocClient.class);
        DashdocSyncService subject = service(client);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> subject.synchronize(
                LocalDate.parse("2026-11-20"),
                LocalDate.parse("2026-11-27")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(client);
    }

    private static DashdocSyncService service(DashdocClient client) {
        return new DashdocSyncService(
            client,
            new DashdocTransportMapper(),
            mock(DriverRepository.class),
            mock(TractorRepository.class),
            mock(SemiTrailerRepository.class),
            mock(ServiceRepository.class),
            mock(TransportFacadeService.class));
    }
}
