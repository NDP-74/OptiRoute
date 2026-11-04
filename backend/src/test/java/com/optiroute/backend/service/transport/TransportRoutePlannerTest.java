package com.optiroute.backend.service.transport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.request.transport.ServiceTiming;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteCostDetailsDto;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.exception.TransportPlanningException;
import com.optiroute.backend.mapper.RouteRequestFactory;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.model.RouteTimeline;
import com.optiroute.backend.service.driver.DriverService;
import com.optiroute.backend.service.route.RouteOptimizationService;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.type.driver.DriverType;

class TransportRoutePlannerTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private final DriverService drivers = mock(DriverService.class);
    private final RouteOptimizationService routing = mock(RouteOptimizationService.class);
    private final TransportRoutePlanner planner = new TransportRoutePlanner(drivers, new RouteRequestFactory(), routing);
    private Driver driver;

    @BeforeEach
    void setUp() {
        driver = new Driver();
        driver.setDriverType(DriverType.LONG_HAUL);
        driver.setLocationLabel("Depot");
        driver.setLocationLatitude(BigDecimal.ZERO);
        driver.setLocationLongitude(BigDecimal.valueOf(5));
        when(drivers.getEntityById(7L)).thenReturn(driver);
        when(routing.calculateRoute(any(), any())).thenAnswer(invocation -> response(invocation.getArgument(0)));
        when(routing.calculateTimeline(any())).thenAnswer(invocation -> timeline(invocation.getArgument(0)));
    }

    @Test
    void shouldSplitTenHoursFromFifteenWithLatestDepartureAtSevenAndArrivalAtNoon() {
        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T12:00:00+01:00", 10), selected(10));

        assertEquals(2, segments.size());
        assertEquals(time("2026-11-03T20:00:00+01:00"), arrival(segments.getFirst()));
        assertEquals(5, segments.getFirst().route().getDuration() / 3600);
        assertEquals(time("2026-11-04T07:00:00+01:00"), departure(segments.getLast()));
        assertEquals(time("2026-11-04T12:00:00+01:00"), arrival(segments.getLast()));
        assertEquals(10 * 3600, segments.stream().mapToLong(s -> s.route().getDuration()).sum());
        assertContinuousAndDaytime(segments);
    }

    @Test
    void shouldResumeBeforeFiveOnlyWhenRequiredAndNeverMoveTheTwentyCut() {
        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T09:00:00+01:00", 10), selected(10));

        assertEquals(time("2026-11-03T20:00:00+01:00"), arrival(segments.getFirst()));
        assertEquals(time("2026-11-04T04:00:00+01:00"), departure(segments.getLast()));
        assertEquals(time("2026-11-04T09:00:00+01:00"), arrival(segments.getLast()));
    }

    @Test
    void shouldRejectWhenDeliveryWouldRequireResumingBeforeMidnight() {
        assertThrows(TransportPlanningException.class, () -> planner.plan(
            request("2026-11-03T15:00:00+01:00", "2026-11-04T04:00:00+01:00", 10), selected(10)));
    }

    @Test
    void shouldHonorAnEarlyMorningDeadlineWithoutMovingTheCut() {
        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T03:00:00+01:00", 7), selected(7));

        assertEquals(time("2026-11-03T20:00:00+01:00"), arrival(segments.getFirst()));
        assertEquals(time("2026-11-04T01:00:00+01:00"), departure(segments.getLast()));
        assertEquals(time("2026-11-04T03:00:00+01:00"), arrival(segments.getLast()));
    }

    @ParameterizedTest
    @EnumSource(DriverType.class)
    void shouldApplyNightRuleToEveryDriverTypeWithoutServiceWindows(DriverType type) {
        driver.setDriverType(type);
        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", null, 10), selected(10));

        assertEquals(2, segments.size());
        assertEquals(time("2026-11-03T20:00:00+01:00"), arrival(segments.getFirst()));
        assertEquals(time("2026-11-04T05:00:00+01:00"), departure(segments.getLast()));
        assertEquals(time("2026-11-04T10:00:00+01:00"), arrival(segments.getLast()));
        assertContinuousAndDaytime(segments);
    }

    @Test
    void shouldSplitEveryNightWithoutWindowsAndWithoutADriver() {
        TransportRequest source = request("2026-11-03T15:00:00+01:00", null, 40);
        source = copy(source, null, source.routeTime(), source.timeMode(), null, source.waypoints());
        var segments = planner.plan(source, selected(40));

        assertEquals(4, segments.size());
        assertEquals(time("2026-11-06T10:00:00+01:00"), arrival(segments.getLast()));
        assertEquals(40 * 3600, segments.stream().mapToLong(s -> s.route().getDuration()).sum());
        assertContinuousAndDaytime(segments);
    }

    @Test
    void shouldScheduleMultipleNightsBackwardsFromDeliveryWithoutDrivingAtNight() {
        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-06T12:00:00+01:00", 40), selected(40));

        assertEquals(4, segments.size());
        assertEquals(time("2026-11-04T07:00:00+01:00"), departure(segments.get(1)));
        assertEquals(time("2026-11-06T12:00:00+01:00"), arrival(segments.getLast()));
        assertContinuousAndDaytime(segments);
    }

    @Test
    void shouldInferDepartureIncludingTheNightWhenOnlyArrivalTimeIsSupplied() {
        TransportRequest source = request("2026-11-04T12:00:00+01:00", null, 25);
        source = copy(source, 7L, source.routeTime(), RouteTimeMode.ARRIVAL, null, List.of());
        var segments = planner.plan(source, selected(25));

        assertEquals(3, segments.size());
        assertEquals(time("2026-11-02T17:00:00+01:00"), departure(segments.getFirst()));
        assertEquals(time("2026-11-04T12:00:00+01:00"), arrival(segments.getLast()));
        assertContinuousAndDaytime(segments);
    }

    @Test
    void shouldRecalculateDepartureWhenTheActualRouteIsLongerThanTheArrivalEstimate() {
        OffsetDateTime deadline = time("2026-11-04T11:00:00+01:00");
        TransportRequest source = copy(request("2026-11-04T09:47:00+01:00", null, 73.0 / 60),
            7L, deadline, RouteTimeMode.ARRIVAL, null, List.of());
        doAnswer(invocation -> responseWithDuration(74 * 60))
            .when(routing).calculateRoute(any(), any());

        var segments = planner.plan(source, selected(73.0 / 60));

        assertEquals(1, segments.size());
        assertEquals(RouteTimeMode.ARRIVAL, segments.getFirst().request().timeMode());
        assertEquals(deadline, segments.getFirst().request().routeTime());
        assertEquals(time("2026-11-04T09:46:00+01:00"), departure(segments.getFirst()));
        assertEquals(deadline, arrival(segments.getFirst()));
        verify(routing, times(2)).calculateRoute(any(), any());
    }

    @Test
    void shouldNotDiscardExplicitDepartureWhenOnlyDeliveryWindowIsPresent() {
        TransportRequest source = request("2026-11-03T15:00:00+01:00", null, 10);
        source = copy(source, 7L, source.routeTime(), RouteTimeMode.DEPARTURE,
            new ServiceTiming(null, null, null, null, time("2026-11-04T12:00:00+01:00"), null), List.of());

        var segments = planner.plan(source, selected(10));

        assertEquals(2, segments.size());
        assertEquals(time("2026-11-03T15:00:00+01:00"), departure(segments.getFirst()));
        assertEquals(time("2026-11-04T07:00:00+01:00"), departure(segments.getLast()));
    }

    @Test
    void shouldPersistAllDaysAsCalculatedRoutesUnderOneService() {
        TransportRequest source = request("2026-11-03T15:00:00+01:00", null, 40);
        var segments = planner.plan(source, selected(40));
        var services = mock(com.optiroute.backend.repository.transport.ServiceRepository.class);
        var routes = mock(com.optiroute.backend.repository.transport.RouteRepository.class);
        when(services.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new ServiceRouteService(services, routes).create(source, segments);
        var capture = ArgumentCaptor.forClass(com.optiroute.backend.entity.transport.Route.class);
        verify(routes, times(4)).save(capture.capture());

        for (int i = 0; i < 4; i++) {
            var route = capture.getAllValues().get(i);
            assertSame(service, route.getService());
            assertEquals(departure(segments.get(i)), route.getDepartureStart());
            assertEquals(arrival(segments.get(i)), route.getArrivalStart());
            assertNull(route.getDepartureEnd());
            assertNull(route.getArrivalEnd());
        }
        assertEquals(40 * 3600, capture.getAllValues().stream().mapToLong(r -> r.getDurationSeconds()).sum());
    }

    @Test
    void shouldShiftNightDepartureToFiveAndKeepExactlyTwentyAsAnArrivalBoundary() {
        for (String start : List.of("2026-11-03T20:00:00+01:00", "2026-11-03T23:00:00+01:00")) {
            var result = planner.plan(request(start, null, 2), selected(2));
            assertEquals(1, result.size());
            assertEquals(time("2026-11-04T05:00:00+01:00"), departure(result.getFirst()));
        }
        var result = planner.plan(request("2026-11-03T18:00:00+01:00", null, 2), selected(2));
        assertEquals(1, result.size());
        assertEquals(time("2026-11-03T20:00:00+01:00"), arrival(result.getFirst()));
        verify(routing, never()).calculateTimeline(any());
    }

    @Test
    void shouldShiftUnconstrainedEarlyDepartureButAllowNecessaryEarlyDeparture() {
        var regular = planner.plan(request("2026-11-03T03:00:00+01:00", null, 2), selected(2));
        assertEquals(time("2026-11-03T05:00:00+01:00"), departure(regular.getFirst()));
        var urgent = planner.plan(request("2026-11-03T03:00:00+01:00", "2026-11-03T05:00:00+01:00", 2), selected(2));
        assertEquals(time("2026-11-03T03:00:00+01:00"), departure(urgent.getFirst()));
    }

    @Test
    void shouldRetainParkingReturnExceptionAfterTwentyForRegional() {
        driver.setDriverType(DriverType.REGIONAL);
        driver.setLocationLongitude(BigDecimal.valueOf(7));
        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T12:00:00+01:00", 10), selected(10));

        assertEquals(2, segments.size());
        assertEquals("Depot", segments.getFirst().request().destinationName());
        assertEquals(time("2026-11-03T22:00:00+01:00"), arrival(segments.getFirst()));
        assertEquals(time("2026-11-04T09:00:00+01:00"), departure(segments.getLast()));
        assertEquals("Depot", segments.getLast().request().originName());
    }

    @Test
    void shouldRequireRegionalParkingEvenWhenDirectDeliveryWouldFitOnDepartureDay() {
        driver.setDriverType(DriverType.REGIONAL);
        driver.setLocationLongitude(BigDecimal.valueOf(2));
        TransportRequest source = request("2026-11-03T10:00:00+01:00", null, 3);
        ServiceTiming timing = new ServiceTiming(45, 45, source.routeTime(), null,
            time("2026-11-04T12:45:00+01:00"), time("2026-11-04T16:45:00+01:00"));
        source = copy(source, 7L, time("2026-11-04T12:45:00+01:00"), RouteTimeMode.ARRIVAL, timing, List.of());

        var segments = planner.plan(source, selected(3));

        assertEquals(2, segments.size());
        assertEquals("Depot", segments.getFirst().request().destinationName());
        assertEquals(time("2026-11-03T10:45:00+01:00"), departure(segments.getFirst()));
        assertEquals(time("2026-11-03T12:45:00+01:00"), arrival(segments.getFirst()));
        assertEquals("Depot", segments.getLast().request().originName());
        assertEquals(time("2026-11-04T11:00:00+01:00"), departure(segments.getLast()));
        assertEquals(time("2026-11-04T12:00:00+01:00"), arrival(segments.getLast()));
        assertSame(timing, segments.getLast().request().serviceTiming());
    }

    @ParameterizedTest
    @CsvSource({
        "07:00, 12:00, 05:00, 08:00",
        "07:00, 07:30, 04:30, 07:30",
        "04:00, 10:00, 05:00, 08:00",
        "12:00, 16:00, 09:00, 12:00"
    })
    void shouldPreferMinimumArrivalAndUseWindowBeforeAllowingDepartureBeforeFive(
            String minimum, String maximum, String expectedDeparture, String expectedArrival) {
        driver.setDriverType(DriverType.REGIONAL);
        driver.setLocationLongitude(BigDecimal.valueOf(2));
        TransportRequest source = request("2026-11-03T10:00:00+01:00", null, 5);
        source = copy(source, 7L, source.routeTime(), RouteTimeMode.DEPARTURE,
            new ServiceTiming(null, null, source.routeTime(), null,
                time("2026-11-04T" + minimum + ":00+01:00"),
                time("2026-11-04T" + maximum + ":00+01:00")), List.of());

        var segments = planner.plan(source, selected(5));

        assertEquals(2, segments.size());
        assertEquals(time("2026-11-03T10:00:00+01:00"), departure(segments.getFirst()));
        assertEquals(time("2026-11-04T" + expectedDeparture + ":00+01:00"), departure(segments.getLast()));
        assertEquals(time("2026-11-04T" + expectedArrival + ":00+01:00"), arrival(segments.getLast()));
    }

    @ParameterizedTest
    @EnumSource(value = DriverType.class, names = {"LONG_HAUL", "SHORT_DISTANCE"})
    void shouldKeepDriverStopRulesWhileTargetingMinimumArrival(DriverType type) {
        driver.setDriverType(type);
        TransportRequest source = request("2026-11-03T15:00:00+01:00", null, 10);
        source = copy(source, 7L, source.routeTime(), RouteTimeMode.DEPARTURE,
            new ServiceTiming(null, null, source.routeTime(), null,
                time("2026-11-04T12:00:00+01:00"), time("2026-11-04T16:00:00+01:00")), List.of());

        var segments = planner.plan(source, selected(10));

        assertEquals(2, segments.size());
        assertEquals(type == DriverType.SHORT_DISTANCE ? "Depot" : "Arrêt de nuit à 20 h",
            segments.getFirst().request().destinationName());
        assertEquals(time("2026-11-03T20:00:00+01:00"), arrival(segments.getFirst()));
        assertEquals(time("2026-11-04T07:00:00+01:00"), departure(segments.getLast()));
        assertEquals(time("2026-11-04T12:00:00+01:00"), arrival(segments.getLast()));
    }

    @Test
    void shouldTargetMinimumArrivalAcrossSeveralNights() {
        TransportRequest source = request("2026-11-03T15:00:00+01:00", null, 40);
        source = copy(source, 7L, source.routeTime(), RouteTimeMode.DEPARTURE,
            new ServiceTiming(null, null, source.routeTime(), null,
                time("2026-11-06T12:00:00+01:00"), time("2026-11-06T16:00:00+01:00")), List.of());

        var segments = planner.plan(source, selected(40));

        assertEquals(4, segments.size());
        assertEquals(time("2026-11-04T07:00:00+01:00"), departure(segments.get(1)));
        assertEquals(time("2026-11-06T12:00:00+01:00"), arrival(segments.getLast()));
        assertContinuousAndDaytime(segments);
    }

    @Test
    void shouldAllowNextMorningAfterRegionalParkingReturnJustAfterMidnight() {
        driver.setDriverType(DriverType.REGIONAL);
        driver.setLocationLongitude(BigDecimal.TEN);
        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T12:00:00+01:00", 11), selected(11));

        assertEquals(time("2026-11-04T01:00:00+01:00"), arrival(segments.getFirst()));
        assertEquals(time("2026-11-04T11:00:00+01:00"), departure(segments.getLast()));
    }

    @Test
    void shouldRejectMissingParkingAndAnUnreachableRegionalDestination() {
        driver.setDriverType(DriverType.REGIONAL);
        driver.setLocationLatitude(null);
        assertThrows(TransportPlanningException.class, () -> planner.plan(
            request("2026-11-03T15:00:00+01:00", null, 40), selected(40)));
        driver.setLocationLatitude(BigDecimal.ZERO);
        assertThrows(TransportPlanningException.class, () -> planner.plan(
            request("2026-11-03T15:00:00+01:00", null, 40), selected(40)));
    }

    @Test
    void shouldLoadOnceUnloadOnlyAtFinalDestinationAndPreserveSourceTimingAndWaypoints() {
        TransportRequest source = request("2026-11-03T14:15:00+01:00", "2026-11-04T12:45:00+01:00", 10);
        ServiceTiming timing = new ServiceTiming(45, 45, source.routeTime(), null, null, source.serviceTiming().arrivalEnd());
        source = copy(source, 7L, source.routeTime(), RouteTimeMode.DEPARTURE, timing,
            List.of(new Position(0, 2), new Position(0, 5), new Position(0, 8)));
        var segments = planner.plan(source, selected(10));

        assertEquals(time("2026-11-03T15:00:00+01:00"), departure(segments.getFirst()));
        assertEquals(time("2026-11-04T07:00:00+01:00"), departure(segments.getLast()));
        assertEquals(time("2026-11-04T12:00:00+01:00"), arrival(segments.getLast()));
        assertEquals(List.of(2.0), segments.getFirst().request().waypoints().stream().map(Position::getLng).toList());
        assertEquals(List.of(8.0), segments.getLast().request().waypoints().stream().map(Position::getLng).toList());
        assertSame(timing, segments.getFirst().request().serviceTiming());
        assertSame(timing, segments.getLast().request().serviceTiming());
    }

    @Test
    void shouldPreserveModeAndParisCutAcrossDaylightSavingChange() {
        TransportRequest source = request("2026-10-24T13:00:00Z", "2026-10-25T11:00:00Z", 10);
        RouteDto selected = selected(10);
        selected.setType(GpsModeType.CHEAPEST);
        var segments = planner.plan(source, selected);

        assertEquals(time("2026-10-24T20:00:00+02:00"), arrival(segments.getFirst()));
        assertEquals(time("2026-10-25T07:00:00+01:00").toInstant(), departure(segments.getLast()).toInstant());
        verify(routing, atLeastOnce()).calculateRoute(any(), eq(GpsModeType.CHEAPEST));
        var capture = ArgumentCaptor.forClass(RouteRequest.class);
        verify(routing).calculateTimeline(capture.capture());
        assertEquals(GpsModeType.CHEAPEST, capture.getValue().getMode());
    }

    @Test
    void shouldRejectRecalculationThatMovesFirstDepartureBeforeAvailability() {
        doAnswer(invocation -> {
            RouteRequest request = invocation.getArgument(0);
            RoutesResponse response = response(request);
            if (request.getTimeMode() == RouteTimeMode.ARRIVAL && request.getDestination().getLng() == 5) {
                response.getRoutes().getFirst().setDuration(6 * 3600);
            }
            return response;
        }).when(routing).calculateRoute(any(), any());
        assertThrows(TransportPlanningException.class, () -> planner.plan(
            request("2026-11-03T15:00:00+01:00", null, 10), selected(10)));
    }

    @Test
    void shouldUseNearbyShortDistanceParkingAndPreferTheLowerDeliveryBound() {
        driver.setDriverType(DriverType.SHORT_DISTANCE);
        driver.setLocationLatitude(new BigDecimal("0.1"));
        driver.setLocationLongitude(BigDecimal.valueOf(6));
        TransportRequest source = request("2026-11-03T15:00:00+01:00", "2026-11-04T12:45:00+01:00", 10);
        source = copy(source, 7L, source.routeTime(), source.timeMode(),
            new ServiceTiming(null, 45, source.routeTime(), null, time("2026-11-04T10:00:00+01:00"),
                time("2026-11-04T12:45:00+01:00")), List.of(new Position(0, 2), new Position(0, 8)));
        RouteDto selected = selected(10);
        selected.setType(GpsModeType.CHEAPEST);

        var segments = planner.plan(source, selected);

        assertEquals(2, segments.size());
        assertEquals("Depot", segments.getFirst().request().destinationName());
        assertEquals("Depot", segments.getLast().request().originName());
        assertEquals(0.1, segments.getFirst().request().destinationLat());
        assertEquals(6, segments.getFirst().request().destinationLng());
        assertEquals(time("2026-11-03T21:00:00+01:00"), arrival(segments.getFirst()));
        assertEquals(time("2026-11-04T05:15:00+01:00"), departure(segments.getLast()));
        assertEquals(time("2026-11-04T09:15:00+01:00"), arrival(segments.getLast()));
        assertEquals(List.of(2.0), segments.getFirst().request().waypoints().stream().map(Position::getLng).toList());
        assertEquals(List.of(8.0), segments.getLast().request().waypoints().stream().map(Position::getLng).toList());
        verify(routing, atLeastOnce()).calculateRoute(any(), eq(GpsModeType.CHEAPEST));
        verify(routing, times(1)).calculateTimeline(any());
    }

    @Test
    void shouldAcceptExactlyFiveButFallBackToLongHaulWhenParkingNeedsAnEarlierDeparture() {
        driver.setDriverType(DriverType.SHORT_DISTANCE);
        driver.setLocationLongitude(BigDecimal.valueOf(4));
        var accepted = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T11:00:00+01:00", 10), selected(10));
        assertEquals("Depot", accepted.getLast().request().originName());
        assertEquals(time("2026-11-04T05:00:00+01:00"), departure(accepted.getLast()));

        var rejected = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T09:00:00+01:00", 10), selected(10));
        assertEquals("Arrêt de nuit à 20 h", rejected.getFirst().request().destinationName());
        assertEquals(time("2026-11-03T20:00:00+01:00"), arrival(rejected.getFirst()));
        assertEquals(time("2026-11-04T04:00:00+01:00"), departure(rejected.getLast()));
    }

    @ParameterizedTest
    @ValueSource(doubles = {49_999, 50_000, 50_001})
    void shouldApplyStrictFiftyKilometerThreshold(double meters) {
        driver.setDriverType(DriverType.SHORT_DISTANCE);
        driver.setLocationLatitude(BigDecimal.valueOf(Math.toDegrees(meters / 6_371_000)));
        driver.setLocationLongitude(BigDecimal.valueOf(6));

        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T12:00:00+01:00", 10), selected(10));

        assertEquals(meters < 50_000 ? "Depot" : "Arrêt de nuit à 20 h",
            segments.getFirst().request().destinationName());
    }

    @Test
    void shouldFallBackWithMissingParkingWithoutTryingAStationRoute() {
        driver.setDriverType(DriverType.SHORT_DISTANCE);
        driver.setLocationLatitude(null);

        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", null, 10), selected(10));

        assertEquals("Arrêt de nuit à 20 h", segments.getFirst().request().destinationName());
        verify(routing, times(3)).calculateRoute(any(), any());
    }

    @Test
    void shouldNotAddParkingDetourToASameDayTrip() {
        driver.setDriverType(DriverType.SHORT_DISTANCE);

        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-03T19:00:00+01:00", 4), selected(4));

        assertEquals(1, segments.size());
        assertEquals("Destination", segments.getFirst().request().destinationName());
        verify(routing, never()).calculateTimeline(any());
        verify(routing, times(1)).calculateRoute(any(), any());
    }

    @Test
    void shouldConsiderParkingEvenWhenDirectDrivingEndsBeforeTwentyButDeliveryIsTomorrow() {
        driver.setDriverType(DriverType.SHORT_DISTANCE);
        driver.setLocationLongitude(BigDecimal.valueOf(2));

        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T12:00:00+01:00", 4), selected(4));

        assertEquals(2, segments.size());
        assertEquals("Depot", segments.getFirst().request().destinationName());
        assertEquals(time("2026-11-04T10:00:00+01:00"), departure(segments.getLast()));
    }

    @Test
    void shouldRejectParkingWhoseRoadAccessMakesNextDayDepartureImpossible() {
        driver.setDriverType(DriverType.SHORT_DISTANCE);
        driver.setLocationLongitude(BigDecimal.valueOf(6));
        doAnswer(invocation -> {
            RouteRequest request = invocation.getArgument(0);
            RoutesResponse result = response(request);
            if (request.getDestination().getLng() == 6) {
                result.getRoutes().getFirst().setDuration(20 * 3600);
            }
            return result;
        }).when(routing).calculateRoute(any(), any());

        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", "2026-11-04T12:00:00+01:00", 10), selected(10));

        assertEquals("Arrêt de nuit à 20 h", segments.getFirst().request().destinationName());
        assertEquals(time("2026-11-04T07:00:00+01:00"), departure(segments.getLast()));
    }

    @Test
    void shouldReevaluateStationOnSubsequentNightsWithoutDiscardingPreviousSegments() {
        driver.setDriverType(DriverType.SHORT_DISTANCE);
        driver.setLocationLongitude(BigDecimal.valueOf(25));

        var segments = planner.plan(request("2026-11-03T15:00:00+01:00", null, 40), selected(40));

        assertEquals(3, segments.size());
        assertEquals("Arrêt de nuit à 20 h", segments.getFirst().request().destinationName());
        assertEquals("Depot", segments.get(1).request().destinationName());
        assertEquals("Depot", segments.getLast().request().originName());
        assertEquals(time("2026-11-05T05:00:00+01:00"), departure(segments.getLast()));
        assertEquals(40 * 3600, segments.stream().mapToLong(s -> s.route().getDuration()).sum());
    }

    @Test
    void shouldSurfaceProviderFailureDuringParkingCalculationRatherThanSilentlyFallingBack() {
        driver.setDriverType(DriverType.SHORT_DISTANCE);
        doAnswer(invocation -> {
            RouteRequest request = invocation.getArgument(0);
            if (request.getDestination().getLng() == 5) {
                throw new IllegalStateException("Provider unavailable");
            }
            return response(request);
        }).when(routing).calculateRoute(any(), any());

        assertThrows(IllegalStateException.class, () -> planner.plan(
            request("2026-11-03T15:00:00+01:00", "2026-11-04T12:00:00+01:00", 10), selected(10)));
    }

    private TransportRequest request(String departure, String delivery, double hours) {
        ServiceTiming timing = delivery == null ? null
            : new ServiceTiming(null, null, time(departure), null, null, time(delivery));
        return new TransportRequest("Mission", 7L, null, null, null, false, "external", "MANUAL",
            time(departure), RouteTimeMode.DEPARTURE, "Origin", null, 0, 0, "Destination", null, 0, hours,
            BigDecimal.valueOf(1000), timing, List.of());
    }

    private TransportRequest copy(TransportRequest source, Long driverId, OffsetDateTime time, RouteTimeMode mode,
            ServiceTiming timing, List<Position> waypoints) {
        return new TransportRequest(source.name(), driverId, null, null, null, false, null, "MANUAL",
            time, mode, source.originName(), null, 0, 0, source.destinationName(), null, 0, source.destinationLng(),
            source.revenue(), timing, waypoints);
    }

    private RoutesResponse response(RouteRequest request) {
        RoutesResponse response = new RoutesResponse();
        response.setRoutes(List.of(selected(timeline(request).durationSeconds() / 3600.0)));
        return response;
    }

    private RoutesResponse responseWithDuration(long durationSeconds) {
        RoutesResponse response = new RoutesResponse();
        response.setRoutes(List.of(selected(durationSeconds / 3600.0)));
        return response;
    }

    private RouteDto selected(double hours) {
        RouteDto route = new RouteDto();
        route.setType(GpsModeType.FASTEST);
        route.setDuration(Math.round(hours * 3600));
        route.setDistanceMeters(Math.round(hours * 100000));
        route.setCosts(new RouteCostDetailsDto());
        route.setPolyline("geometry");
        return route;
    }

    private RouteTimeline timeline(RouteRequest request) {
        List<Position> points = new ArrayList<>();
        points.add(request.getOrigin());
        if (request.getWaypoints() != null) points.addAll(request.getWaypoints());
        points.add(request.getDestination());
        List<RouteTimeline.Portion> portions = new ArrayList<>();
        long duration = 0;
        for (int i = 1; i < points.size(); i++) {
            long seconds = Math.round(Math.abs(points.get(i).getLng() - points.get(i - 1).getLng()) * 3600);
            portions.add(new RouteTimeline.Portion(List.of(points.get(i - 1), points.get(i)), seconds, i - 1));
            duration += seconds;
        }
        return new RouteTimeline(portions, duration);
    }

    private OffsetDateTime departure(TransportRouteSegment segment) {
        return segment.request().timeMode() == RouteTimeMode.ARRIVAL
            ? segment.request().routeTime().minusSeconds(segment.route().getDuration()) : segment.request().routeTime();
    }

    private OffsetDateTime arrival(TransportRouteSegment segment) {
        return departure(segment).plusSeconds(segment.route().getDuration());
    }

    private OffsetDateTime time(String value) {
        return OffsetDateTime.parse(value);
    }

    private void assertContinuousAndDaytime(List<TransportRouteSegment> segments) {
        for (int i = 0; i < segments.size(); i++) {
            var segment = segments.get(i);
            assertTrue(departure(segment).atZoneSameInstant(PARIS).getHour() >= 5);
            assertFalse(arrival(segment).atZoneSameInstant(PARIS).toLocalTime().isAfter(java.time.LocalTime.of(20, 0)));
            assertEquals(departure(segment).atZoneSameInstant(PARIS).toLocalDate(),
                arrival(segment).atZoneSameInstant(PARIS).toLocalDate());
            if (i > 0) {
                assertEquals(segments.get(i - 1).request().destinationLng(), segment.request().originLng(), 0.000001);
                assertTrue(departure(segment).isAfter(arrival(segments.get(i - 1))));
            }
        }
    }
}
