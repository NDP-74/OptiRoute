package com.optiroute.backend.service.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

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

        when(routeRepository.findByDriverIdAndDepartureStartGreaterThanEqualAndDepartureStartLessThan(eq(1L),any(),any())).thenReturn(List.of(targetRoute,otherRoute));
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