package com.optiroute.backend.service.transport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.request.transport.ServiceTiming;
import com.optiroute.backend.dto.request.transport.TransportFromRouteRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteCostDetailsDto;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.mapper.RouteRequestFactory;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.model.RouteTimeline;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.transport.ServiceRepository;
import com.optiroute.backend.service.driver.DriverService;
import com.optiroute.backend.service.route.RouteOptimizationService;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.type.driver.DriverType;

class TransportFacadeServiceTest {

    @ParameterizedTest
    @CsvSource({"automatic,LONG_HAUL", "from-route,LONG_HAUL", "update,LONG_HAUL",
        "automatic,SHORT_DISTANCE", "from-route,SHORT_DISTANCE", "update,SHORT_DISTANCE"})
    void shouldPersistBothSegmentsThroughEveryFacadeEntry(String entry, DriverType driverType) {
        DriverService drivers = mock(DriverService.class);
        Driver driver = new Driver();
        driver.setDriverType(driverType);
        driver.setLocationLabel("Depot");
        driver.setLocationLatitude(new java.math.BigDecimal("47.5"));
        driver.setLocationLongitude(new java.math.BigDecimal("2.5"));
        when(drivers.getEntityById(7L)).thenReturn(driver);
        RouteOptimizationService routing = mock(RouteOptimizationService.class);
        when(routing.calculateTimeline(any())).thenReturn(new RouteTimeline(List.of(
            new RouteTimeline.Portion(List.of(new Position(48, 2), new Position(46, 4)), 28800, 0)
        ), 28800));
        RoutesResponse fullResponse = new RoutesResponse();
        fullResponse.setRoutes(List.of(route(28800)));
        when(routing.calculateRoute(any(RouteRequest.class))).thenReturn(fullResponse);
        RoutesResponse first = new RoutesResponse();
        first.setRoutes(List.of(route(7200)));
        RoutesResponse second = new RoutesResponse();
        second.setRoutes(List.of(route(10800)));
        when(routing.calculateRoute(any(), any())).thenAnswer(invocation -> {
            RouteRequest calculation = invocation.getArgument(0);
            return calculation.getOrigin().getLat() == 48
                ? calculation.getDestination().getLat() == 46 ? fullResponse : first : second;
        });
        ServiceRepository services = mock(ServiceRepository.class);
        RouteRepository routes = mock(RouteRepository.class);
        when(services.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(services.findById(1L)).thenReturn(Optional.of(new Service()));
        when(routes.findByServiceIdOrderByIdAsc(1L)).thenReturn(List.of());
        RouteRequestFactory factory = new RouteRequestFactory();
        TransportFacadeService facade = new TransportFacadeService(new ServiceRouteService(services, routes),
            factory, routing, new TransportRoutePlanner(drivers, factory, routing));
        OffsetDateTime departure = OffsetDateTime.parse("2026-10-29T18:00:00+01:00");
        TransportRequest request = new TransportRequest("Mission", 7L, null, null, null, false, null, "MANUAL",
            departure, RouteTimeMode.DEPARTURE, "Origine", null, 48, 2, "Livraison", null, 46, 4, null,
            new ServiceTiming(null, null, departure, null, null, departure.plusDays(1).minusHours(7)), List.of());
        TransportFromRouteRequest fromRoute = new TransportFromRouteRequest(request, route(28800));

        Service saved = switch (entry) {
            case "automatic" -> facade.createTransport(request);
            case "from-route" -> facade.createFromRoute(fromRoute);
            case "update" -> facade.updateFromRoute(1L, fromRoute);
            default -> throw new AssertionError(entry);
        };

        var capture = ArgumentCaptor.forClass(Route.class);
        verify(routes, times(2)).save(capture.capture());
        Route firstSaved = capture.getAllValues().getFirst();
        Route secondSaved = capture.getAllValues().getLast();
        assertSame(saved, firstSaved.getService());
        assertSame(saved, secondSaved.getService());
        assertEquals(departure.withHour(20), firstSaved.getArrivalStart());
        assertEquals(firstSaved.getDestinationLat(), secondSaved.getOriginLat());
        assertEquals(departure.plusDays(1).withHour(8), secondSaved.getDepartureStart());
        if (driverType == DriverType.SHORT_DISTANCE) {
            assertEquals("Depot", firstSaved.getDestinationName());
            assertEquals(47.5, firstSaved.getDestinationLat());
            assertEquals("Depot", secondSaved.getOriginName());
        }
    }

    private RouteDto route(long duration) {
        RouteDto route = new RouteDto();
        route.setDuration(duration);
        route.setCosts(new RouteCostDetailsDto());
        return route;
    }
}
