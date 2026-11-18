package com.optiroute.backend.service.transport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.optiroute.backend.dto.request.transport.RouteSegmentRequest;
import com.optiroute.backend.dto.request.transport.ServiceInfoRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;
import com.optiroute.backend.dto.response.route.RouteCostDetailsDto;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.entity.event.Event;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.exception.TransportPlanningException;
import com.optiroute.backend.mapper.RouteRequestFactory;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.repository.EventRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.transport.ServiceRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;

import jakarta.validation.Validation;

class ServiceRouteServiceTest {
    private final ServiceRepository services = mock(ServiceRepository.class);
    private final RouteRepository routes = mock(RouteRepository.class);
    private final EventRepository events = mock(EventRepository.class);
    private final TractorRepository tractors = mock(TractorRepository.class);
    private final SemiTrailerRepository semiTrailers = mock(SemiTrailerRepository.class);
    private final ServiceRouteService subject = new ServiceRouteService(services, routes, events, tractors, semiTrailers);
    private final JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @Test
    void shouldDefaultRevenueToZeroAndNormalizeNullAssignment() {
        Service service = new Service();

        assertEquals(BigDecimal.ZERO, service.getRevenue());
        service.setRevenue(null);
        assertEquals(BigDecimal.ZERO, service.getRevenue());
    }

    @Test
    void shouldSaveIndependentServiceAndRouteWindowsAndOrderedWaypoints() throws Exception {
        TransportRequest request = mapper.readValue("""
            {
              "name": "Mission",
              "routeTime": "2026-10-29T10:00:00+01:00",
              "timeMode": "ARRIVAL",
              "serviceTiming": {
                "loadingDurationMinutes": 45, "unloadingDurationMinutes": 30,
                "departureStart": "2026-10-29T06:00:00+01:00",
                "arrivalStart": "2026-10-29T10:00:00+01:00",
                "arrivalEnd": "2026-10-30T12:00:00+01:00"
              },
              "waypoints": [
                {"lat": 48, "lng": 2, "name": "Premier", "address": "Adresse 1"},
                {"lat": 47, "lng": 3, "name": "Second"}
              ]
            }
            """, TransportRequest.class);
        when(services.save(any(Service.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RouteDto estimate = estimate();

        Service saved = subject.create(request, estimate);

        assertEquals(45, saved.getLoadingDurationMinutes());
        assertEquals(30, saved.getUnloadingDurationMinutes());
        assertEquals(request.serviceTiming().departureStart(), saved.getDepartureStart());
        assertEquals(request.serviceTiming().arrivalEnd(), saved.getArrivalEnd());
        ArgumentCaptor<Route> capture = ArgumentCaptor.forClass(Route.class);
        verify(routes).save(capture.capture());
        Route route = capture.getValue();
        assertSame(saved, route.getService());
        assertEquals(request.routeTime().minusSeconds(900), route.getDepartureStart());
        assertEquals(request.routeTime(), route.getArrivalStart());
        assertEquals(List.of(48.0, 47.0), route.getWaypoints().stream().map(Position::getLat).toList());
        String json = mapper.writeValueAsString(route.getWaypoints());
        Position[] restored = mapper.readValue(json, Position[].class);
        assertEquals("Premier", restored[0].getName());
        assertEquals("Adresse 1", restored[0].getAddress());
        assertEquals(47.0, restored[1].getLat());

        var calculation = new RouteRequestFactory().fromTransport(request);
        assertEquals(RouteTimeMode.ARRIVAL, calculation.getTimeMode());
        assertEquals(request.routeTime(), calculation.getRouteTime());
        assertEquals(2, calculation.getWaypoints().size());
    }

    @Test
    void shouldCalculateOnlyMinimumRouteTimesAndKeepStoredWaypointsOnManualUpdate() throws Exception {
        TransportRequest request = mapper.readValue("""
            {"routeTime": "2026-10-29T05:00:00+01:00", "timeMode": "DEPARTURE"}
            """, TransportRequest.class);
        Service existing = new Service();
        existing.setLoadingDurationMinutes(30);
        Route route = new Route();
        route.setWaypoints(List.of(new Position(48, 2)));
        when(services.findById(1L)).thenReturn(Optional.of(existing));
        when(services.save(existing)).thenReturn(existing);
        when(routes.findByServiceIdOrderByIdAsc(1L)).thenReturn(List.of(route));

        subject.update(1L, request, estimate());

        assertEquals(30, existing.getLoadingDurationMinutes());
        assertEquals(1, route.getWaypoints().size());
        assertEquals(request.routeTime(), route.getDepartureStart());
        assertEquals(request.routeTime().plusSeconds(900), route.getArrivalStart());
        var calculation = new RouteRequestFactory().fromTransport(request);
        assertEquals(RouteTimeMode.DEPARTURE, calculation.getTimeMode());
        assertEquals(request.routeTime(), calculation.getRouteTime());
        assertTrue(calculation.getWaypoints().isEmpty());
    }

    @Test
    void shouldKeepDashdocSourceWindowsOnServiceAndUseTheirStartTimesOnRoute() throws Exception {
        TransportRequest request = mapper.readValue("""
            {
              "externalId": "dashdoc-123",
              "externalSource": "DASHDOC",
              "routeTime": "2026-10-29T10:00:00Z",
              "timeMode": "ARRIVAL",
              "serviceTiming": {
                "departureStart": "2026-10-29T05:00:00Z",
                "departureEnd": "2026-10-29T06:00:00Z",
                "arrivalStart": "2026-10-29T10:00:00Z",
                "arrivalEnd": "2026-10-29T12:00:00Z"
              }
            }
            """, TransportRequest.class);
        when(services.save(any(Service.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Service service = subject.create(request, estimate());

        assertEquals("dashdoc-123", service.getExternalId());
        assertEquals("DASHDOC", service.getExternalSource());
        ArgumentCaptor<Route> capture = ArgumentCaptor.forClass(Route.class);
        verify(routes).save(capture.capture());
        Route route = capture.getValue();
        assertEquals(request.serviceTiming().departureStart(), route.getDepartureStart());
        assertEquals(request.serviceTiming().arrivalStart(), route.getArrivalStart());
        assertEquals(request.serviceTiming().departureEnd(), service.getDepartureEnd());
        assertEquals(request.serviceTiming().arrivalEnd(), service.getArrivalEnd());
    }

    @Test
    void shouldAllowExplicitlyClearingUserWaypoints() throws Exception {
        TransportRequest request = mapper.readValue("{\"waypoints\": []}", TransportRequest.class);
        Service existing = new Service();
        Route route = new Route();
        route.setWaypoints(List.of(new Position(48, 2)));
        when(services.findById(1L)).thenReturn(Optional.of(existing));
        when(services.save(existing)).thenReturn(existing);
        when(routes.findByServiceIdOrderByIdAsc(1L)).thenReturn(List.of(route));

        subject.update(1L, request, estimate());

        assertTrue(route.getWaypoints().isEmpty());
    }

    @Test
    void shouldReturnServiceTimingAndUserWaypointsInDetailResponse() {
        Service service = new Service();
        service.setLoadingDurationMinutes(45);
        Route route = new Route();
        route.setId(1L);
        route.setWaypoints(List.of(new Position(48, 2)));
        route.setArrivalStart(java.time.OffsetDateTime.parse("2026-10-29T10:00:00Z"));
        when(services.findById(1L)).thenReturn(Optional.of(service));
        when(routes.findByServiceIdOrderByIdAsc(1L)).thenReturn(List.of(route));
        TransportCostService costs = mock(TransportCostService.class);
        CostCategoryResponse emptyCosts = new CostCategoryResponse(List.of(), 0);
        when(costs.calculate(any(Route.class))).thenReturn(
            new TransportCostDetailsResponse(emptyCosts, emptyCosts, emptyCosts, 0));
        TransportDetailService details = new TransportDetailService(
            services, routes, null, null, null, null, costs);

        var response = details.getDetail(1L);

        assertEquals(45, response.serviceTiming().loadingDurationMinutes());
        assertEquals(route.getArrivalStart(), response.arrivalStart());
        assertEquals(48.0, response.waypoints().getFirst().getLat());
        assertEquals(1, response.routes().size());
        route.setWaypoints(null);
        assertTrue(details.getDetail(1L).waypoints().isEmpty());
    }

    @Test
    void shouldReturnAllRoutesAndKeepSelectedRouteCostsAndRevenueInSummary() {
        Service service = new Service();
        service.setRevenue(new java.math.BigDecimal("1000.00"));
        Route first = new Route();
        first.setId(11L);
        first.setDistanceMeters(300_000L);
        Route second = new Route();
        second.setId(12L);
        second.setDistanceMeters(500_000L);
        when(services.findById(1L)).thenReturn(Optional.of(service));
        when(routes.findByServiceIdOrderByIdAsc(1L)).thenReturn(List.of(first, second));
        CostCategoryResponse firstVehicle = new CostCategoryResponse(List.of(), 100);
        CostCategoryResponse secondVehicle = new CostCategoryResponse(List.of(), 200);
        CostCategoryResponse noCosts = new CostCategoryResponse(List.of(), 0);
        TransportCostService costs = mock(TransportCostService.class);
        when(costs.calculate(first)).thenReturn(new TransportCostDetailsResponse(firstVehicle, noCosts, noCosts, 100));
        when(costs.calculate(second)).thenReturn(new TransportCostDetailsResponse(secondVehicle, noCosts, noCosts, 200));
        TransportDetailService details = new TransportDetailService(
            services, routes, null, null, null, null, costs);

        var response = details.getDetail(1L, 12L);

        assertEquals(12L, response.routeId());
        assertEquals(2, response.routeNumber());
        assertEquals(2, response.routeCount());
        assertEquals(625.0, response.revenue().doubleValue());
        assertEquals(200.0, response.costs().totalCost());
        assertEquals(2, response.routes().size());
        assertEquals(11L, response.routes().getFirst().routeId());
        assertEquals(100.0, response.routes().getFirst().costs().vehicle().totalCost());
        assertEquals(12L, response.routes().get(1).routeId());
        assertEquals(200.0, response.routes().get(1).costs().vehicle().totalCost());
    }

    @Test
    void shouldSaveSeveralRouteSegmentsUnderTheSameService() throws Exception {
        TransportRequest firstRequest = mapper.readValue("""
            {"originName":"Départ","destinationName":"Stationnement","originLat":48.0,"originLng":2.0,
             "destinationLat":47.0,"destinationLng":3.0,"routeTime":"2026-10-29T08:00:00+01:00",
             "timeMode":"DEPARTURE"}
            """, TransportRequest.class);
        TransportRequest secondRequest = mapper.readValue("""
            {"originName":"Stationnement","destinationName":"Livraison","originLat":47.0,"originLng":3.0,
             "destinationLat":46.0,"destinationLng":4.0,"routeTime":"2026-10-30T10:30:00+01:00",
             "timeMode":"ARRIVAL"}
            """, TransportRequest.class);
        when(services.save(any(Service.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Service service = subject.create(firstRequest, List.of(
            new TransportRouteSegment(firstRequest, estimate(), true),
            new TransportRouteSegment(secondRequest, estimate(), true)));

        ArgumentCaptor<Route> capture = ArgumentCaptor.forClass(Route.class);
        verify(routes, times(2)).save(capture.capture());
        List<Route> savedRoutes = capture.getAllValues();
        assertSame(service, savedRoutes.get(0).getService());
        assertSame(service, savedRoutes.get(1).getService());
        assertEquals("Stationnement", savedRoutes.get(0).getDestinationName());
        assertEquals("Stationnement", savedRoutes.get(1).getOriginName());
        assertEquals(secondRequest.routeTime().minusSeconds(900), savedRoutes.get(1).getDepartureStart());
        assertEquals(secondRequest.routeTime(), savedRoutes.get(1).getArrivalStart());
    }

    @Test
    void shouldRejectNegativeDurationsInvertedWindowsAndInvalidWaypoints() throws Exception {
        TransportRequest invalid = mapper.readValue("""
            {
              "serviceTiming": {
                "loadingDurationMinutes": -1,
                "arrivalStart": "2026-10-30T10:00:00Z",
                "arrivalEnd": "2026-10-29T10:00:00Z"
              },
              "waypoints": [{"lat": 91, "lng": 2}, null]
            }
            """, TransportRequest.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertEquals(4, factory.getValidator().validate(invalid).size());
            assertTrue(factory.getValidator().validate(mapper.readValue("{}", TransportRequest.class)).isEmpty());
        }
    }

    @Test
    void shouldMoveEventsToTheReplannedSegmentCarryingTheirPointOrDayBeforeDeletingSurplusRoutes() throws Exception {
        TransportRequest firstRequest = mapper.readValue("""
            {"tractorId": 7, "routeTime": "2026-10-29T08:00:00+01:00", "timeMode": "DEPARTURE",
             "waypoints": [{"lat": 48, "lng": 2}]}
            """, TransportRequest.class);
        TransportRequest secondRequest = mapper.readValue("""
            {"tractorId": 7, "routeTime": "2026-10-30T08:00:00+01:00", "timeMode": "DEPARTURE",
             "waypoints": [{"lat": 46, "lng": 4, "eventId": 101}]}
            """, TransportRequest.class);
        Service existing = new Service();
        Route first = new Route();
        first.setId(11L);
        Route second = new Route();
        second.setId(12L);
        Route third = new Route();
        third.setId(13L);
        Tractor oldTractor = new Tractor();
        oldTractor.setId(3L);
        Tractor newTractor = new Tractor();
        newTractor.setId(7L);
        Event withPoint = Event.builder().id(101L).route(first).tractor(oldTractor).build();
        Event withoutPoint = Event.builder().id(102L).route(third).tractor(oldTractor)
            .eventDate(java.time.LocalDateTime.parse("2026-10-30T07:00:00")).build();
        when(services.findById(1L)).thenReturn(Optional.of(existing));
        when(services.save(existing)).thenReturn(existing);
        when(routes.findByServiceIdOrderByIdAsc(1L)).thenReturn(List.of(first, second, third));
        when(routes.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(events.findByRouteIdInOrderByRouteIdAscIdAsc(List.of(11L, 12L, 13L))).thenReturn(List.of(withPoint, withoutPoint));
        when(tractors.getReferenceById(7L)).thenReturn(newTractor);

        subject.update(1L, firstRequest, List.of(
            new TransportRouteSegment(firstRequest, estimate(), true),
            new TransportRouteSegment(secondRequest, estimate(), true)));

        assertSame(second, withPoint.getRoute());
        assertSame(second, withoutPoint.getRoute());
        assertSame(newTractor, withPoint.getTractor());
        assertEquals(second.getDepartureStart().toLocalDateTime(), withPoint.getEventDate());
        assertTrue(second.getWaypoints().isEmpty(), "event points must not be stored as user waypoints");
        assertEquals(1, first.getWaypoints().size());
        var order = inOrder(events, routes);
        order.verify(events).saveAll(List.of(withPoint, withoutPoint));
        order.verify(routes).deleteAll(List.of(third));
    }

    @Test
    void shouldRefuseReplanningThatRemovesTheSemiTrailerOfAnEvent() throws Exception {
        TransportRequest request = mapper.readValue("{\"tractorId\": 7}", TransportRequest.class);
        Service existing = new Service();
        Route route = new Route();
        route.setId(11L);
        SemiTrailer semiTrailer = new SemiTrailer();
        semiTrailer.setId(5L);
        Event event = Event.builder().id(101L).route(route).semiTrailer(semiTrailer).build();
        when(services.findById(1L)).thenReturn(Optional.of(existing));
        when(services.save(existing)).thenReturn(existing);
        when(routes.findByServiceIdOrderByIdAsc(1L)).thenReturn(List.of(route));
        when(routes.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(events.findByRouteIdInOrderByRouteIdAscIdAsc(List.of(11L))).thenReturn(List.of(event));

        assertThrows(TransportPlanningException.class, () -> subject.update(1L, request, estimate()));
    }

    @Test
    void shouldUpdateOnlyTheEditedSegmentAndKeepItsEvents() throws Exception {
        Service service = new Service();
        service.setId(1L);
        Route route = new Route();
        route.setId(12L);
        route.setService(service);
        Event event = Event.builder().id(101L).route(route).build();
        RouteDto selected = estimate();
        selected.setType(GpsModeType.CHEAPEST);
        RouteSegmentRequest request = new RouteSegmentRequest(4L, 7L, null, true,
            java.time.OffsetDateTime.parse("2026-10-30T10:00:00+01:00"), RouteTimeMode.ARRIVAL,
            "Départ", "Adresse départ", 48, 2, "Arrivée", "Adresse arrivée", 46, 4,
            List.of(position(47, 3, null), position(46.5, 3.5, 101L)), selected);
        when(routes.findById(12L)).thenReturn(Optional.of(route));
        when(routes.save(route)).thenReturn(route);
        when(events.findByRouteIdOrderByIdAsc(12L)).thenReturn(List.of(event));

        subject.updateRoute(1L, 12L, request);

        assertEquals(GpsModeType.CHEAPEST, route.getRouteType());
        assertEquals(request.routeTime(), route.getArrivalStart());
        assertEquals(request.routeTime().minusSeconds(900), route.getDepartureStart());
        assertEquals(List.of(47.0), route.getWaypoints().stream().map(Position::getLat).toList());
        assertEquals("Départ", route.getOriginName());
        assertTrue(route.isEmptyTrip());
        assertSame(route, event.getRoute());
        assertEquals(route.getDepartureStart().toLocalDateTime(), event.getEventDate());
        verify(routes, times(1)).save(any());
        verify(services, never()).save(any());
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> subject.updateRoute(2L, 12L, request));
    }

    @Test
    void shouldUpdateServiceInformationWithoutTouchingRoutes() {
        Service service = new Service();
        service.setName("Ancien");
        when(services.findById(1L)).thenReturn(Optional.of(service));

        subject.updateInfo(1L, new ServiceInfoRequest("Client", 9L, new java.math.BigDecimal("1500.00"), 30, 45));

        assertEquals("Client", service.getName());
        assertEquals(9L, service.getCustomerId());
        assertEquals(new java.math.BigDecimal("1500.00"), service.getRevenue());
        assertEquals(30, service.getLoadingDurationMinutes());
        assertEquals(45, service.getUnloadingDurationMinutes());
        verify(services).save(service);
        verifyNoInteractions(routes);
    }

    private Position position(double lat, double lng, Long eventId) {
        Position position = new Position(lat, lng);
        position.setEventId(eventId);
        return position;
    }

    @Test
    void shouldDeleteSurplusRoutesWithoutEventsOnUpdate() throws Exception {
        TransportRequest request = mapper.readValue("{}", TransportRequest.class);
        Service existing = new Service();
        Route kept = new Route();
        kept.setId(11L);
        Route removed = new Route();
        removed.setId(12L);
        when(services.findById(1L)).thenReturn(Optional.of(existing));
        when(services.save(existing)).thenReturn(existing);
        when(routes.findByServiceIdOrderByIdAsc(1L)).thenReturn(List.of(kept, removed));

        subject.update(1L, request, estimate());

        verify(routes).deleteAll(List.of(removed));
    }

    private RouteDto estimate() {
        RouteDto estimate = new RouteDto();
        estimate.setDuration(900L);
        estimate.setDistanceMeters(12000L);
        estimate.setPolyline("polyline");
        RouteCostDetailsDto costs = new RouteCostDetailsDto();
        costs.setFuelCost(12.5);
        costs.setTollCost(3.0);
        estimate.setCosts(costs);
        return estimate;
    }
}
