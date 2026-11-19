package com.optiroute.backend.service.dashdoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.type.RouteTimeMode;

class DashdocTransportMapperTest {

    private final DashdocTransportMapper mapper = new DashdocTransportMapper();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void mapsSingleDeliveryUsingNormalizedDriverAndVehicleIdentity() throws Exception {
        JsonNode transport = jsonMapper.readTree("""
            {
              "uid": "dashdoc-uid-1",
              "sequential_id": 1234,
              "agreed_price_total": 875.50,
              "deliveries": [{
                "origin": {
                  "address": {"name": "Entrepôt", "address": "1 rue A", "city": "Lyon", "latitude": 45.75, "longitude": 4.85},
                  "slots": [{"start": "2026-11-20T07:00:00+01:00", "end": "2026-11-20T09:00:00+01:00"}]
                },
                "destination": {
                  "address": {"name": "Client", "address": "2 rue B", "city": "Paris", "latitude": 48.85, "longitude": 2.35},
                  "slots": [{"start": "2026-11-20T15:00:00+01:00", "end": "2026-11-20T18:00:00+01:00"}]
                }
              }],
              "segments": [{
                "trucker": {"display_name": " ELODIE  DUPONT "},
                "vehicle": {"license_plate": "ab 123 cd"},
                "trailers": [],
                "sort_order": 0,
                "origin": {
                  "address": {"name": "Origine segment", "address": "3 rue C", "city": "Avignon", "latitude": 44.12312, "longitude": 5.18025}
                },
                "destination": {
                  "address": {"name": "Destination segment", "address": "4 rue D", "city": "Marseille", "latitude": 43.2965, "longitude": 5.3698}
                },
                "scheduled_start_range": {"start": "2026-11-20T08:00:00Z", "end": "2026-11-20T09:00:00Z"},
                "scheduled_start": "2026-11-20T08:00:00Z",
                "scheduled_end_range": {"start": "2026-11-20T10:00:00Z", "end": "2026-11-20T10:00:00Z"}
              }]
            }
            """);
        Driver driver = new Driver();
        driver.setId(7L);
        driver.setFirstName("Élodie");
        driver.setLastName("Dupont");
        Tractor tractor = new Tractor();
        tractor.setId(9L);
        tractor.setRegistration("AB-123-CD");

        var request = mapper.map(
            transport,
            LocalDate.parse("2026-11-20"),
            LocalDate.parse("2026-11-26"),
            List.of(driver),
            List.of(tractor),
            List.of(),
            45,
            45);

        assertEquals("dashdoc-uid-1", request.externalId());
        assertEquals("DASHDOC", request.externalSource());
        assertEquals(7L, request.driverId());
        assertEquals(9L, request.tractorId());
        assertNull(request.semiTrailerId());
        assertEquals(RouteTimeMode.DEPARTURE, request.timeMode());
        assertEquals(OffsetDateTime.parse("2026-11-20T08:00:00Z"), request.routeTime());
        assertEquals(875.50, request.revenue().doubleValue());
        assertEquals("3 rue C, Avignon", request.originAddress());
        assertEquals(44.12312, request.originLat());
        assertEquals(5.18025, request.originLng());
        assertEquals("4 rue D, Marseille", request.destinationAddress());
        assertEquals(43.2965, request.destinationLat());
        assertEquals(5.3698, request.destinationLng());
        assertEquals(45, request.serviceTiming().loadingDurationMinutes());
        assertEquals(OffsetDateTime.parse("2026-11-20T08:00:00Z"), request.serviceTiming().departureStart());
        assertEquals(OffsetDateTime.parse("2026-11-20T09:00:00Z"), request.serviceTiming().departureEnd());
        assertEquals(OffsetDateTime.parse("2026-11-20T10:00:00Z"), request.serviceTiming().arrivalStart());
        assertEquals(request.serviceTiming().arrivalStart(), request.serviceTiming().arrivalEnd());
    }

    @Test
    void skipsWhenNoDriverMappingExists() throws Exception {
        JsonNode transport = jsonMapper.readTree("""
            {
              "uid": "dashdoc-uid-2",
              "deliveries": [{
                "origin": {"address": {"latitude": 45, "longitude": 4}, "slots": [{"start": "2026-11-20T07:00:00Z", "end": "2026-11-20T08:00:00Z"}]},
                "destination": {"address": {"latitude": 48, "longitude": 2}, "slots": [{"start": "2026-11-20T15:00:00Z", "end": "2026-11-20T16:00:00Z"}]}
              }],
              "segments": [{
                "trucker": {"display_name": "Inconnu"},
                "vehicle": {"license_plate": "AB123CD"},
                "trailers": [],
                "origin": {"address": {"latitude": 45, "longitude": 4}},
                "destination": {"address": {"latitude": 48, "longitude": 2}},
                "scheduled_start_range": {"start": "2026-11-20T07:00:00Z", "end": "2026-11-20T08:00:00Z"},
                "scheduled_end_range": {"start": "2026-11-20T15:00:00Z", "end": "2026-11-20T16:00:00Z"}
              }]
            }
            """);
        Tractor tractor = new Tractor();
        tractor.setId(9L);
        tractor.setRegistration("AB123CD");

        DashdocTransportMappingException exception = assertThrows(
            DashdocTransportMappingException.class,
            () -> mapper.map(transport, LocalDate.parse("2026-11-20"), LocalDate.parse("2026-11-20"),
                List.of(), List.of(tractor), List.of(), 45, 45));

        assertEquals("Correspondance conducteur introuvable ou ambiguë", exception.getMessage());
    }

    @Test
    void skipsTransportsWithMultipleDeliveries() throws Exception {
        JsonNode transport = jsonMapper.readTree("""
            {"uid": "dashdoc-uid-3", "deliveries": [{}, {}], "segments": []}
            """);

        DashdocTransportMappingException exception = assertThrows(
            DashdocTransportMappingException.class,
            () -> mapper.map(transport, LocalDate.parse("2026-11-20"), LocalDate.parse("2026-11-20"),
                List.of(), List.of(), List.of(), 45, 45));

        assertEquals("Le transport ne contient pas exactement une livraison", exception.getMessage());
    }

    @Test
    void filtersOutTransportsWhoseDeliveryWindowsAreOutsideTheSelectedRange() throws Exception {
        JsonNode transport = jsonMapper.readTree("""
            {
              "uid": "dashdoc-uid-4",
              "deliveries": [{"origin": {}, "destination": {}}],
              "segments": [{
                "scheduled_start_range": {"start": "2026-11-20T07:00:00Z", "end": "2026-11-20T08:00:00Z"},
                "scheduled_end_range": {"start": "2026-11-20T15:00:00Z", "end": "2026-11-20T16:00:00Z"}
              }]
            }
            """);

        assertThrows(
            DashdocTransportOutsideRangeException.class,
            () -> mapper.map(transport, LocalDate.parse("2026-11-21"), LocalDate.parse("2026-11-21"),
                List.of(), List.of(), List.of(), 45, 45));
    }

    @Test
    void filtersByScheduledRangesUsingParisDatesAcrossDaylightSavingChanges() throws Exception {
        JsonNode transport = jsonMapper.readTree("""
            {
              "uid": "dashdoc-dst",
              "deliveries": [{
                "origin": {"address": {"latitude": 45, "longitude": 4}},
                "destination": {"address": {"latitude": 48, "longitude": 2}}
              }],
              "segments": [{
                "sort_order": 0,
                "origin": {"address": {"latitude": 45, "longitude": 4}},
                "destination": {"address": {"latitude": 48, "longitude": 2}},
                "scheduled_start_range": {"start": "2026-03-28T23:30:00Z", "end": "2026-03-29T01:30:00Z"},
                "scheduled_start": "2026-03-29T01:15:00Z",
                "scheduled_end_range": {"start": "2026-03-29T02:00:00Z", "end": "2026-03-29T02:30:00Z"},
                "trucker": {"display_name": "Camille Martin"},
                "vehicle": {"license_plate": "AB123CD"},
                "trailers": []
              }]
            }
            """);
        Driver driver = new Driver();
        driver.setId(7L);
        driver.setFirstName("Camille");
        driver.setLastName("Martin");
        Tractor tractor = new Tractor();
        tractor.setId(9L);
        tractor.setRegistration("AB123CD");

        var request = mapper.map(
            transport,
            LocalDate.parse("2026-03-29"),
            LocalDate.parse("2026-03-29"),
            List.of(driver),
            List.of(tractor),
            List.of(),
            45,
            45);

        assertEquals(OffsetDateTime.parse("2026-03-29T01:15:00Z"), request.routeTime());
        assertEquals(OffsetDateTime.parse("2026-03-29T03:15+02:00"), request.routeTime().atZoneSameInstant(
            java.time.ZoneId.of("Europe/Paris")).toOffsetDateTime());
        assertThrows(
            DashdocTransportOutsideRangeException.class,
            () -> mapper.map(transport, LocalDate.parse("2026-03-28"), LocalDate.parse("2026-03-28"),
                List.of(driver), List.of(tractor), List.of(), 45, 45));
    }
}
