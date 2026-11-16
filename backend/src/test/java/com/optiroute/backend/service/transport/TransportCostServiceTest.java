package com.optiroute.backend.service.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.optiroute.backend.dto.request.cost.CostCalculationContext;
import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.event.Event;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.repository.EventRepository;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.service.cost.CostParameterEngine;
import com.optiroute.backend.service.cost.DriverCostService;
import com.optiroute.backend.service.cost.StructureCostService;
import com.optiroute.backend.service.cost.VehicleCostService;
import com.optiroute.backend.service.cost.WorkingDaysService;
import com.optiroute.backend.type.driver.DriverCostType;

class TransportCostServiceTest {

    @Test
    void shouldIncludeServiceAndEventDurationsInDriverSalaryProration() {
        RouteRepository routeRepository = mock(RouteRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        TractorRepository tractorRepository = mock(TractorRepository.class);
        SemiTrailerRepository semiTrailerRepository = mock(SemiTrailerRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        CostParameterEngine costParameterEngine = mock(CostParameterEngine.class);
        VehicleCostService vehicleCostService = mock(VehicleCostService.class);
        StructureCostService structureCostService = mock(StructureCostService.class);
        DriverCostService driverCostService = new DriverCostService(new WorkingDaysService(), costParameterEngine);
        TransportCostService service = new TransportCostService(routeRepository, driverRepository, tractorRepository, semiTrailerRepository, driverCostService, vehicleCostService,
            structureCostService, eventRepository, costParameterEngine);

        OffsetDateTime departure = OffsetDateTime.parse("2026-10-12T08:00:00+02:00");
        Route targetRoute = route(10L,departure,3600L,10L);
        targetRoute.getService().setLoadingDurationMinutes(30);
        targetRoute.getService().setUnloadingDurationMinutes(30);
        Route otherRoute = route(11L,departure.plusHours(4),10800L,11L);
        Event event = Event.builder().durationMinutes(60).build();
        Driver driver = new Driver();
        driver.setAnnualSalary(BigDecimal.valueOf(26000));
        driver.setCostType(DriverCostType.FIXED);

        when(routeRepository.findByDriverIdAndDepartureStartGreaterThanEqualAndDepartureStartLessThan(eq(1L),
            eq(departure.toLocalDate().atStartOfDay(java.time.ZoneId.of("Europe/Paris")).toOffsetDateTime()),any())).thenReturn(List.of(targetRoute,otherRoute));
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(eventRepository.findByRouteIdOrderByIdAsc(10L)).thenReturn(List.of(event));
        when(eventRepository.findByRouteIdOrderByIdAsc(11L)).thenReturn(List.of());
        when(costParameterEngine.calculateCosts(any(),any(CostCalculationContext.class),anyList())).thenReturn(List.of());
        when(costParameterEngine.calculateManualCost(any(),any(CostCalculationContext.class))).thenReturn(null);
        when(vehicleCostService.calculateCosts(isNull(),isNull(),any(CostCalculationContext.class),eq(0.0),eq(0.0),anyList())).thenReturn(new CostCategoryResponse(List.of(), 0));
        when(structureCostService.calculateCosts(any(CostCalculationContext.class),anyList())).thenReturn(new CostCategoryResponse(List.of(), 0));

        var costs = service.calculate(targetRoute);

        double dailySalary = driver.getAnnualSalary().doubleValue() / new WorkingDaysService().getWorkingDaysInYear(2026);
        assertEquals(dailySalary * 0.5,costs.driver().totalCost(),0.01);
    }

    @ParameterizedTest
    @CsvSource({
        "45.0, 6.0, false, false, true",
        "45.000001, 6.0, false, false, false",
        "45.0, 6.000001, false, false, false",
        "45.0, 6.0, true, false,",
        "45.0, 6.0, false, true,"
    })
    void shouldUseLastArrivalForEveryDailyContext(double destinationLat, double destinationLng, boolean missingParking, boolean missingDriver, Boolean expected) {
        RouteRepository routeRepository = mock(RouteRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        VehicleCostService vehicleCostService = mock(VehicleCostService.class);
        StructureCostService structureCostService = mock(StructureCostService.class);
        DriverCostService driverCostService = mock(DriverCostService.class);
        TransportCostService service = new TransportCostService(routeRepository, driverRepository, mock(TractorRepository.class),
            mock(SemiTrailerRepository.class), driverCostService, vehicleCostService, structureCostService, eventRepository, mock(CostParameterEngine.class));
        OffsetDateTime departure = OffsetDateTime.parse("2026-11-16T08:00:00+01:00");
        Route targetRoute = route(10L,departure,3600L,10L);
        // The last arrival is after midnight, even though its departure precedes the other evening trip.
        Route lastRoute = route(11L,departure.plusHours(14),10800L,11L);
        lastRoute.setDestinationLat(destinationLat);
        lastRoute.setDestinationLng(destinationLng);
        Route eveningRoute = route(12L,departure.plusHours(15),1800L,12L);
        Driver driver = new Driver();
        if (!missingParking) {
            driver.setLocationLatitude(new BigDecimal("45.000000"));
            driver.setLocationLongitude(new BigDecimal("6.000000"));
        }
        when(driverRepository.findById(1L)).thenReturn(missingDriver ? Optional.empty() : Optional.of(driver));
        when(routeRepository.findByDriverIdAndDepartureStartGreaterThanEqualAndDepartureStartLessThan(eq(1L),
            eq(departure.toLocalDate().atStartOfDay(java.time.ZoneId.of("Europe/Paris")).toOffsetDateTime()),any()))
            .thenReturn(List.of(lastRoute,targetRoute,eveningRoute));
        CostCategoryResponse emptyCosts = new CostCategoryResponse(List.of(), 0);
        when(vehicleCostService.calculateCosts(isNull(),isNull(),any(),eq(0.0),eq(0.0),anyList())).thenReturn(emptyCosts);
        when(driverCostService.calculateCosts(any(),any(),anyList())).thenReturn(emptyCosts);
        when(structureCostService.calculateCosts(any(),anyList())).thenReturn(emptyCosts);

        service.calculate(targetRoute);

        ArgumentCaptor<CostCalculationContext> context = ArgumentCaptor.forClass(CostCalculationContext.class);
        verify(structureCostService).calculateCosts(context.capture(), org.mockito.ArgumentMatchers.<List<CostCalculationContext>>argThat(
            contexts -> contexts.size() == 3 && contexts.stream().allMatch(c -> java.util.Objects.equals(expected, c.driverEndsDayAtParking()))));
        assertEquals(expected, context.getValue().driverEndsDayAtParking());
    }

    @Test
    void shouldBuildSeparateTriggerDaysWithTwoDayCountsAcrossMonthBoundary() {
        RouteRepository routeRepository = mock(RouteRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        VehicleCostService vehicleCosts = mock(VehicleCostService.class);
        StructureCostService structureCosts = mock(StructureCostService.class);
        TransportCostService service = new TransportCostService(routeRepository,driverRepository,mock(TractorRepository.class),
            mock(SemiTrailerRepository.class),mock(DriverCostService.class),vehicleCosts,structureCosts,mock(EventRepository.class),mock(CostParameterEngine.class));
        OffsetDateTime departure = OffsetDateTime.parse("2026-12-01T08:00:00+01:00");
        Route current = route(20L,departure,3600L,20L);
        Route previous = route(19L,departure.minusDays(1),3600L,19L);
        Route next = route(21L,departure.plusDays(1),3600L,21L);
        Route nextSecond = route(22L,departure.plusDays(1).plusHours(2),3600L,22L);
        when(routeRepository.findByDriverIdAndDepartureStartGreaterThanEqualAndDepartureStartLessThan(eq(1L),any(),any()))
            .thenAnswer(invocation -> {
                OffsetDateTime start = invocation.getArgument(1);
                if (start.toLocalDate().equals(previous.getDepartureStart().toLocalDate())) return List.of(previous);
                if (start.toLocalDate().equals(next.getDepartureStart().toLocalDate())) return List.of(next,nextSecond);
                return List.of(current);
            });
        CostCategoryResponse empty = new CostCategoryResponse(List.of(),0);
        when(vehicleCosts.calculateCosts(isNull(),isNull(),any(),eq(0.0),eq(0.0),anyList())).thenReturn(empty);
        when(structureCosts.calculateCosts(any(),anyList())).thenReturn(empty);

        service.calculate(current);

        ArgumentCaptor<CostCalculationContext> context = ArgumentCaptor.forClass(CostCalculationContext.class);
        verify(structureCosts).calculateCosts(context.capture(), org.mockito.ArgumentMatchers.<List<CostCalculationContext>>argThat(
            contexts -> contexts.size() == 2
                && contexts.getFirst().date().equals(previous.getDepartureStart().toLocalDate())
                && contexts.getFirst().nightTransportCount() == 2
                && contexts.getFirst().dailyTransportCount() == 1));
        assertEquals(3,context.getValue().nightTransportCount());
        assertEquals(1,context.getValue().dailyTransportCount());
    }

    private Route route(Long id, OffsetDateTime departure, Long durationSeconds, Long serviceId) {
        Service service = new Service();
        service.setId(serviceId);
        Route route = new Route();
        route.setId(id);
        route.setService(service);
        route.setDriverId(1L);
        route.setDepartureStart(departure);
        route.setArrivalStart(departure.plusSeconds(durationSeconds));
        route.setDurationSeconds(durationSeconds);
        route.setDistanceMeters(10000L);
        return route;
    }
}