package com.optiroute.backend.service.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.service.cost.DriverCostService;
import com.optiroute.backend.service.cost.VehicleCostService;

class TransportPlanningServiceTest {

    @Test
    void shouldAllocateServiceRevenueAcrossRouteDistances() {
        RouteRepository routes = mock(RouteRepository.class);
        DriverRepository drivers = mock(DriverRepository.class);
        TransportCostService transportCosts = mock(TransportCostService.class);
        TractorRepository tractors = mock(TractorRepository.class);
        SemiTrailerRepository semiTrailers = mock(SemiTrailerRepository.class);
        Route first = route(11L, 300_000L);
        Route second = route(12L, 500_000L);
        when(routes.findByDepartureStartGreaterThanEqualAndDepartureStartLessThan(any(), any()))
            .thenReturn(List.of(first, second));
        when(routes.findByServiceIdIn(List.of(1L))).thenReturn(List.of(first, second));
        when(drivers.findAll()).thenReturn(List.of());
        when(tractors.findAll()).thenReturn(List.of());
        when(semiTrailers.findAll()).thenReturn(List.of());
        var emptyCategory = new CostCategoryResponse(List.of(), 0);
        when(transportCosts.calculate(first)).thenReturn(
            new TransportCostDetailsResponse(emptyCategory, emptyCategory, emptyCategory, 100));
        when(transportCosts.calculate(second)).thenReturn(
            new TransportCostDetailsResponse(emptyCategory, emptyCategory, emptyCategory, 200));

        TransportPlanningService subject = new TransportPlanningService(routes, drivers, transportCosts,
            mock(DriverCostService.class), tractors, semiTrailers, mock(VehicleCostService.class));

        var response = subject.getPlanning(LocalDate.parse("2026-10-29"), LocalDate.parse("2026-10-31"));

        assertEquals(375.0, response.transports().get(0).revenue());
        assertEquals(625.0, response.transports().get(1).revenue());
        assertEquals(1, response.transports().get(0).routeNumber());
        assertEquals(2, response.transports().get(0).routeCount());
        assertEquals(2, response.transports().get(1).routeNumber());
        assertEquals(2, response.transports().get(1).routeCount());
        assertEquals(100.0, response.transports().get(0).totalCost());
        assertEquals(200.0, response.transports().get(1).totalCost());
        assertEquals(1000.0, response.transports().stream().mapToDouble(transport -> transport.revenue()).sum());
    }

    private Route route(Long routeId, long distanceMeters) {
        Service service = new Service();
        service.setId(1L);
        service.setName("Mission multi-jours");
        service.setRevenue(new BigDecimal("1000.00"));
        Route route = new Route();
        route.setId(routeId);
        route.setService(service);
        route.setDistanceMeters(distanceMeters);
        route.setDriverId(null);
        route.setDepartureStart(OffsetDateTime.parse("2026-10-29T08:00:00+02:00"));
        route.setArrivalStart(OffsetDateTime.parse("2026-10-29T12:00:00+02:00"));
        return route;
    }
}
