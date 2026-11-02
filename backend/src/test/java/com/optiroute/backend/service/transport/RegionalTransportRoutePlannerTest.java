package com.optiroute.backend.service.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.request.transport.ServiceTiming;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.mapper.RouteRequestFactory;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.service.driver.DriverService;
import com.optiroute.backend.service.route.RouteOptimizationService;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.type.driver.DriverType;

class RegionalTransportRoutePlannerTest {

    private final DriverService drivers = mock(DriverService.class);
    private final RouteOptimizationService routing = mock(RouteOptimizationService.class);
    private final RegionalTransportRoutePlanner planner = new RegionalTransportRoutePlanner(
        drivers, new RouteRequestFactory(), routing);

    @Test
    void shouldSplitNextDayRegionalServiceAtDriversParking() {
        Driver driver = new Driver();
        driver.setDriverType(DriverType.REGIONAL);
        driver.setLocationLabel("Dépôt");
        driver.setLocationLatitude(new BigDecimal("48.5"));
        driver.setLocationLongitude(new BigDecimal("2.5"));
        when(drivers.getEntityById(7L)).thenReturn(driver);
        when(drivers.getEntityById(7L)).thenReturn(driver);
        RouteDto estimate = estimate();
        RoutesResponse response = new RoutesResponse();
        response.setRoutes(List.of(estimate));
        when(routing.calculateRoute(any(RouteRequest.class))).thenReturn(response);

        TransportRequest request = request();
        List<TransportRouteSegment> segments = planner.plan(request, estimate);

        assertEquals(2, segments.size());
        assertEquals("Dépôt", segments.get(0).request().destinationName());
        assertEquals(48.5, segments.get(0).request().destinationLat());
        assertEquals(1, segments.get(0).request().waypoints().size());
        assertEquals("Dépôt", segments.get(1).request().originName());
        assertEquals("Livraison", segments.get(1).request().destinationName());
        assertEquals(OffsetDateTime.parse("2026-10-30T10:30:00+01:00"), segments.get(1).request().routeTime());
        assertEquals(RouteTimeMode.ARRIVAL, segments.get(1).request().timeMode());
        assertEquals(2, segments.stream().filter(TransportRouteSegment::calculatedTimes).count());

        ArgumentCaptor<RouteRequest> routeRequests = ArgumentCaptor.forClass(RouteRequest.class);
        verify(routing, times(2)).calculateRoute(routeRequests.capture());
        assertEquals(OffsetDateTime.parse("2026-10-29T08:00:00+01:00"), routeRequests.getAllValues().get(0).getRouteTime());
        assertEquals(OffsetDateTime.parse("2026-10-30T10:30:00+01:00"), routeRequests.getAllValues().get(1).getRouteTime());
        assertEquals(RouteTimeMode.ARRIVAL, routeRequests.getAllValues().get(1).getTimeMode());
    }

    @Test
    void shouldKeepSingleRouteForNonRegionalDriver() {
        when(drivers.getEntityById(7L)).thenReturn(driver(DriverType.LONG_HAUL));
        RouteDto selectedRoute = estimate();

        List<TransportRouteSegment> segments = planner.plan(request(), selectedRoute);

        assertEquals(1, segments.size());
        assertSame(selectedRoute, segments.getFirst().route());
        assertFalse(segments.getFirst().calculatedTimes());
        verifyNoInteractions(routing);
    }

    private TransportRequest request() {
        ServiceTiming timing = new ServiceTiming(null, 30,
            OffsetDateTime.parse("2026-10-29T08:00:00+01:00"), null, null,
            OffsetDateTime.parse("2026-10-30T11:00:00+01:00"));
        return new TransportRequest("Mission", 7L, null, null, null, false, null, "MANUAL",
            OffsetDateTime.parse("2026-10-29T08:00:00+01:00"), RouteTimeMode.DEPARTURE,
            "Origine", null, 48, 2, "Livraison", null, 46, 4, null, timing,
            List.of(new Position(47.5, 3.0)));
    }

    private Driver driver(DriverType type) {
        Driver driver = new Driver();
        driver.setDriverType(type);
        driver.setLocationLabel("Dépôt");
        driver.setLocationLatitude(new BigDecimal("48.5"));
        driver.setLocationLongitude(new BigDecimal("2.5"));
        return driver;
    }

    private RouteDto estimate() {
        RouteDto estimate = new RouteDto();
        estimate.setDuration(900);
        estimate.setDistanceMeters(12000);
        estimate.setPolyline("polyline");
        var costs = new com.optiroute.backend.dto.response.route.RouteCostDetailsDto();
        costs.setFuelCost(12.5);
        costs.setTollCost(3.0);
        estimate.setCosts(costs);
        return estimate;
    }
}
