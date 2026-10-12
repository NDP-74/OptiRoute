package com.optiroute.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.response.route.RouteCostDetailsDto;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.entity.event.Event;
import com.optiroute.backend.entity.event.PointOfInterest;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.repository.EventRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.service.route.RouteOptimizationService;
import com.optiroute.backend.type.GpsModeType;

import jakarta.persistence.EntityNotFoundException;

class EventServiceTest {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final RouteOptimizationService optimizationService = mock(RouteOptimizationService.class);
    private final EventService service = new EventService(eventRepository, routeRepository, null, null, null, null, optimizationService);

    @Test
    void shouldDeleteAndFlushBeforeRecalculatingWithRemainingWaypoints() {
        Route route = prepareRoute(List.of(locatedEvent()));

        service.delete(1L);

        var order = inOrder(eventRepository, optimizationService, routeRepository);
        order.verify(eventRepository).findById(1L);
        order.verify(eventRepository).delete(any(Event.class));
        order.verify(eventRepository).flush();
        order.verify(eventRepository).findByRouteIdOrderByIdAsc(10L);
        order.verify(optimizationService).calculateRoute(any(RouteRequest.class), eq(GpsModeType.CHEAPEST));
        order.verify(routeRepository).save(route);

        RouteRequest request = capturedRequest();
        assertEquals(1, request.getWaypoints().size());
        assertEquals(48.0, request.getWaypoints().getFirst().getLat());
        assertEquals(2.0, request.getWaypoints().getFirst().getLng());
        assertEquals(route.getStartDate(), request.getRouteTime());
        assertEquals(route.getTractorId(), request.getTractorId());
        assertEquals(12000L, route.getDistanceMeters());
        assertEquals(900L, route.getDurationSeconds());
        assertEquals("updated-polyline", route.getPolyline());
        assertEquals(BigDecimal.valueOf(12.5), route.getFuelCost());
        assertEquals(BigDecimal.valueOf(3.0), route.getTollCost());
        assertEquals(route.getStartDate().plusSeconds(900), route.getEndDate());
    }

    @Test
    void shouldRecalculateDirectRouteWhenLastEventIsDeleted() {
        prepareRoute(List.of());

        service.delete(1L);

        assertEquals(List.of(), capturedRequest().getWaypoints());
    }

    @Test
    void shouldIgnoreRemainingEventsWithoutCoordinates() {
        prepareRoute(List.of(new Event(), Event.builder().pointOfInterest(new PointOfInterest()).build()));

        service.delete(1L);

        assertEquals(List.of(), capturedRequest().getWaypoints());
    }

    @Test
    void shouldRejectUnknownEventWithoutDeletingOrRecalculating() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.delete(1L));

        verifyNoInteractions(optimizationService, routeRepository);
        verify(eventRepository).findById(1L);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void shouldPropagateRecalculationFailureWithoutSavingRoute() {
        prepareRoute(List.of());
        when(optimizationService.calculateRoute(any(RouteRequest.class), eq(GpsModeType.CHEAPEST)))
                .thenThrow(new IllegalStateException("Routing unavailable"));

        assertThrows(IllegalStateException.class, () -> service.delete(1L));

        verifyNoInteractions(routeRepository);
    }

    private Route prepareRoute(List<Event> remainingEvents) {
        Route route = new Route();
        route.setId(10L);
        route.setRouteType(GpsModeType.CHEAPEST);
        route.setStartDate(OffsetDateTime.parse("2026-10-12T08:00:00+02:00"));
        route.setTractorId(20L);
        Event event = Event.builder().id(1L).route(route).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.findByRouteIdOrderByIdAsc(10L)).thenReturn(remainingEvents);

        RouteCostDetailsDto costs = new RouteCostDetailsDto();
        costs.setFuelCost(12.5);
        costs.setTollCost(3.0);
        RouteDto selected = new RouteDto();
        selected.setDistanceMeters(12000);
        selected.setDuration(900);
        selected.setPolyline("updated-polyline");
        selected.setType(GpsModeType.CHEAPEST);
        selected.setCosts(costs);
        RoutesResponse response = new RoutesResponse();
        response.setRoutes(List.of(selected));
        when(optimizationService.calculateRoute(any(RouteRequest.class), eq(GpsModeType.CHEAPEST))).thenReturn(response);
        return route;
    }

    private Event locatedEvent() {
        PointOfInterest poi = new PointOfInterest();
        poi.setLatitude(BigDecimal.valueOf(48));
        poi.setLongitude(BigDecimal.valueOf(2));
        return Event.builder().pointOfInterest(poi).build();
    }

    private RouteRequest capturedRequest() {
        ArgumentCaptor<RouteRequest> captor = ArgumentCaptor.forClass(RouteRequest.class);
        verify(optimizationService).calculateRoute(captor.capture(), eq(GpsModeType.CHEAPEST));
        return captor.getValue();
    }
}
