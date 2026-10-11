package com.optiroute.backend.service.transport;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.optiroute.backend.dto.request.cost.CostCalculationContext;
import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.repository.EventRepository;
import com.optiroute.backend.dto.response.cost.AppliedCostResponse;
import com.optiroute.backend.entity.event.Event;
import com.optiroute.backend.type.cost.CostParameterCategoryType;
import com.optiroute.backend.service.cost.CostParameterEngine;
import com.optiroute.backend.service.cost.DriverCostService;
import com.optiroute.backend.service.cost.StructureCostService;
import com.optiroute.backend.service.cost.VehicleCostService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransportCostService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final TractorRepository tractorRepository;
    private final SemiTrailerRepository semiTrailerRepository;
    private final DriverCostService driverCostService;
    private final VehicleCostService vehicleCostService;
    private final StructureCostService structureCostService;
    private final EventRepository eventRepository;
    private final CostParameterEngine costParameterEngine;

    public TransportCostDetailsResponse calculate(Route route) {
        LocalDate date = route.getStartDate().atZoneSameInstant(ZONE).toLocalDate();

        List<Route> dayRoutes = findDayRoutes(route,date);
        List<CostCalculationContext> dailyContexts = dayRoutes.stream().map(r -> buildContext(r,dayRoutes)).toList();
        CostCalculationContext context = buildContext(route,dayRoutes);

        Driver driver = route.getDriverId() == null ? null : driverRepository.findById(route.getDriverId()).orElse(null);
        Tractor tractor = route.getTractorId() == null ? null : tractorRepository.findById(route.getTractorId()).orElse(null);
        SemiTrailer semiTrailer = route.getSemiTrailerId() == null ? null : semiTrailerRepository.findById(route.getSemiTrailerId()).orElse(null);

        double fuelCost = route.getFuelCost() == null ? 0 : route.getFuelCost().doubleValue();
        double tollCost = route.getTollCost() == null ? 0 : route.getTollCost().doubleValue();

        CostCategoryResponse vehicle = vehicleCostService.calculateCosts(tractor,semiTrailer,context,fuelCost,tollCost,dailyContexts);
        CostCategoryResponse driverCosts = driver == null ? new CostCategoryResponse(List.of(),0) : driverCostService.calculateCosts(driver,context,dailyContexts);
        CostCategoryResponse structure = structureCostService.calculateCosts(context,dailyContexts);

        List<Event> events = route.getId() == null ? List.of() : eventRepository.findByRouteIdOrderByIdAsc(route.getId());
        for (Event event : events) {
            AppliedCostResponse manualCost = costParameterEngine.calculateManualCost(event.getCostParameter(),context);
            if (manualCost == null) {
                continue;
            }
            CostParameterCategoryType category = event.getCostParameter().getCategory();
            if (category == CostParameterCategoryType.VEHICLE) {
                vehicle = withCost(vehicle,manualCost);
            } else if (category == CostParameterCategoryType.DRIVER) {
                driverCosts = withCost(driverCosts,manualCost);
            } else {
                structure = withCost(structure,manualCost);
            }
        }

        return new TransportCostDetailsResponse(vehicle, driverCosts, structure, vehicle.totalCost() + driverCosts.totalCost() + structure.totalCost());
    }

    private CostCategoryResponse withCost(CostCategoryResponse category, AppliedCostResponse cost) {
        List<AppliedCostResponse> costs = new ArrayList<>(category.costs());
        costs.add(cost);
        return new CostCategoryResponse(costs,category.totalCost() + cost.amount());
    }

    // Trajets du même jour pour le même conducteur (le véhicule suit le conducteur)
    private List<Route> findDayRoutes(Route route, LocalDate date) {
        if (route.getDriverId() == null) {
            return List.of(route);
        }

        OffsetDateTime start = date.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
        List<Route> routes = new ArrayList<>(routeRepository.findByDriverIdAndStartDateGreaterThanEqualAndStartDateLessThan(route.getDriverId(),start,end));
        if (routes.stream().noneMatch(r -> r.getId().equals(route.getId()))) {
            routes.add(route);
        }
        return routes;
    }

    private CostCalculationContext buildContext(Route route, List<Route> dayRoutes) {
        double dailyKm = dayRoutes.stream().mapToDouble(r -> r.getDistanceMeters() / 1000.0).sum();
        double dailyHours = dayRoutes.stream().mapToDouble(r -> r.getDurationSeconds() / 3600.0).sum();

        LocalTime dayStart = dayRoutes.stream().map(r -> r.getStartDate().atZoneSameInstant(ZONE).toLocalTime()).min(Comparator.naturalOrder()).orElse(null);
        LocalTime dayEnd = dayRoutes.stream().map(r -> r.getEndDate().atZoneSameInstant(ZONE).toLocalTime()).max(Comparator.naturalOrder()).orElse(null);

        Long trailerTypeId = null;
        if (route.getSemiTrailerId() != null) {
            trailerTypeId = semiTrailerRepository.findById(route.getSemiTrailerId()).map(SemiTrailer::getTrailerType).map(t -> t.getId()).orElse(null);
        }

        return new CostCalculationContext(route.getStartDate().atZoneSameInstant(ZONE).toLocalDate(), route.getDistanceMeters() / 1000.0, dailyKm,
            route.getDurationSeconds() / 3600.0, dayRoutes.size(), route.isEmptyTrip(), route.getStartDate().atZoneSameInstant(ZONE).toLocalTime(),
            route.getEndDate().atZoneSameInstant(ZONE).toLocalTime(), dayStart, dayEnd, trailerTypeId, dailyHours);
    }
}
