package com.optiroute.backend.service.transport;

import com.optiroute.backend.utils.MoneyUtils;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;
import com.optiroute.backend.dto.response.transport.PlanningDriverResponse;
import com.optiroute.backend.dto.response.transport.PlanningResponse;
import com.optiroute.backend.dto.response.transport.PlanningUnassignedVehicleResponse;
import com.optiroute.backend.dto.response.transport.PlanningUnassignedVehiclesResponse;
import com.optiroute.backend.dto.response.transport.TransportPlanningResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.service.cost.DriverCostService;
import com.optiroute.backend.service.cost.VehicleCostService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransportPlanningService {

    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final TransportCostService transportCostService;
    private final DriverCostService driverCostService;
    private final TractorRepository tractorRepository;
    private final SemiTrailerRepository semiTrailerRepository;
    private final VehicleCostService vehicleCostService;

    // Récupére le planning des transports pour une plage de date
    private static final ZoneId PLANNING_ZONE = ZoneId.of("Europe/Paris");

    public PlanningResponse getPlanning(LocalDate startDate, LocalDate endDate) {
        return getPlanning(startDate, endDate, null, true);
    }

    public PlanningResponse getPlanning(LocalDate startDate, LocalDate endDate, List<Long> driverIds, boolean includeUnusedVehicles) {

        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        OffsetDateTime start = startDate.atStartOfDay(PLANNING_ZONE).toOffsetDateTime();
        OffsetDateTime end = endDate.atStartOfDay(PLANNING_ZONE).toOffsetDateTime();

        List<Route> routes = driverIds == null
            ? routeRepository.findByDepartureStartGreaterThanEqualAndDepartureStartLessThan(start, end)
            : driverIds.isEmpty() ? List.of()
            : routeRepository.findByDriverIdInAndDepartureStartGreaterThanEqualAndDepartureStartLessThan(driverIds, start, end);
        Map<Long, List<Route>> routesByServiceId = new HashMap<>();
        if (!routes.isEmpty()) {
            List<Long> serviceIds = routes.stream().map(route -> route.getService().getId()).distinct().toList();
            routeRepository.findByServiceIdIn(serviceIds).stream()
                .sorted(Comparator.comparing(Route::getId))
                .forEach(route -> routesByServiceId.computeIfAbsent(route.getService().getId(), ignored -> new ArrayList<>()).add(route));
        }

        List<TransportCostDetailsResponse> routeCosts = routes.isEmpty() ? List.of() : transportCostService.calculateAll(routes);
        Map<Long, TransportCostDetailsResponse> costsByRouteId = new HashMap<>();
        for (int index = 0; index < routes.size(); index++) {
            costsByRouteId.put(routes.get(index).getId(), routeCosts.get(index));
        }

        List<Driver> availableDrivers = driverIds == null || includeUnusedVehicles ? driverRepository.findAll()
            : driverIds.isEmpty() ? List.of() : driverRepository.findAllById(driverIds);
        Map<Long, Driver> driversById = availableDrivers.stream().collect(Collectors.toMap(Driver::getId, driver -> driver));
        List<Tractor> allTractors = includeUnusedVehicles ? tractorRepository.findAll()
            : routes.isEmpty() ? List.of() : tractorRepository.findAllById(routes.stream().map(Route::getTractorId).filter(java.util.Objects::nonNull).distinct().toList());
        Map<Long, Tractor> tractorsById = allTractors.stream().collect(Collectors.toMap(Tractor::getId, tractor -> tractor));
        List<SemiTrailer> allSemiTrailers = includeUnusedVehicles ? semiTrailerRepository.findAll()
            : routes.isEmpty() ? List.of() : semiTrailerRepository.findAllById(routes.stream().map(Route::getSemiTrailerId).filter(java.util.Objects::nonNull).distinct().toList());
        Map<Long, SemiTrailer> semiTrailersById = allSemiTrailers.stream()
            .collect(Collectors.toMap(SemiTrailer::getId, semiTrailer -> semiTrailer));

        List<TransportPlanningResponse> transports = routes.stream().map(route -> {
            var service = route.getService();
            TransportCostDetailsResponse costs = costsByRouteId.get(route.getId());
            List<Route> serviceRoutes = routesByServiceId.getOrDefault(service.getId(), List.of());
            int routeNumber = 1;
            for (int index = 0; index < serviceRoutes.size(); index++) {
                if (serviceRoutes.get(index).getId().equals(route.getId())) {
                    routeNumber = index + 1;
                    break;
                }
            }
            BigDecimal routeRevenue = ServiceRouteRevenueAllocator.allocate(service.getRevenue(), serviceRoutes, route);
            BigDecimal revenue = routeRevenue == null ? MoneyUtils.ZERO : routeRevenue;

            Driver driver = route.getDriverId() == null ? null : driversById.get(route.getDriverId());
            Tractor tractor = route.getTractorId() == null ? null : tractorsById.get(route.getTractorId());
            SemiTrailer semiTrailer = route.getSemiTrailerId() == null ? null : semiTrailersById.get(route.getSemiTrailerId());
            String tractorRegistration = tractor == null ? null : tractor.getRegistration();
            String semiTrailerRegistration = semiTrailer == null ? null : semiTrailer.getRegistration();

            return new TransportPlanningResponse(service.getId(), route.getId(), routeNumber, serviceRoutes.size(),
                service.getName(), driver != null ? driver.getId() : null,
                driver != null ? driver.getFirstName() + " " + driver.getLastName() : null, tractorRegistration, semiTrailerRegistration,
                route.getDepartureStart(), route.getArrivalStart(),
                route.getOriginName(), route.getDestinationName(), route.isEmptyTrip(), costs.totalCost(), costs.driver().totalCost(), costs.structure().totalCost(),
                costs.vehicle().totalCost(), revenue);
        }).toList();

        Map<Long, Set<LocalDate>> transportDatesByDriver = routes.stream().filter(route -> route.getDriverId() != null && route.getDepartureStart() != null)
            .collect(Collectors.groupingBy(Route::getDriverId,Collectors.mapping(route -> route.getDepartureStart().atZoneSameInstant(PLANNING_ZONE).toLocalDate(),Collectors.toSet())));

        var visibleDrivers = driverIds == null ? availableDrivers.stream() : driverIds.stream().map(driversById::get).filter(java.util.Objects::nonNull);
        List<PlanningDriverResponse> drivers = visibleDrivers
            .map(driver -> new PlanningDriverResponse(driver.getId(), driver.getFirstName() + " " + driver.getLastName(),
                driver.getTractor() != null ? driver.getTractor().getRegistration() : null, driver.getSemiTrailer() != null ? driver.getSemiTrailer().getRegistration() : null,
                driverCostService.calculateSalaryForPeriodExcludingDates(driver,startDate,endDate,transportDatesByDriver.getOrDefault(driver.getId(),Set.of())),
                driver.getCostType(), driver.getDriverType()))
            .toList();

        PlanningUnassignedVehiclesResponse unassignedVehicles = includeUnusedVehicles
            ? buildUnassignedVehicles(availableDrivers, allTractors, allSemiTrailers, startDate, endDate)
            : new PlanningUnassignedVehiclesResponse(List.of(), MoneyUtils.ZERO, List.of());

        return new PlanningResponse(transports, drivers, unassignedVehicles);
    }

    // Véhicules non affectés à un conducteur, avec leur coût d'amortissement cumulé
    // pour la période sélectionnée
    private PlanningUnassignedVehiclesResponse buildUnassignedVehicles(
        List<Driver> allDrivers,
        List<Tractor> allTractors,
        List<SemiTrailer> allSemiTrailers,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Set<Long> assignedTractorIds = allDrivers.stream().map(Driver::getTractor).filter(java.util.Objects::nonNull).map(Tractor::getId).collect(Collectors.toSet());
        Set<Long> assignedSemiTrailerIds = allDrivers.stream().map(Driver::getSemiTrailer).filter(java.util.Objects::nonNull).map(SemiTrailer::getId).collect(Collectors.toSet());

        List<Tractor> unassignedTractors = allTractors.stream().filter(tractor -> !assignedTractorIds.contains(tractor.getId())).toList();
        List<SemiTrailer> unassignedSemiTrailers = allSemiTrailers.stream().filter(semiTrailer -> !assignedSemiTrailerIds.contains(semiTrailer.getId())).toList();

        List<PlanningUnassignedVehicleResponse> vehicles = new java.util.ArrayList<>();
        unassignedTractors.forEach(tractor -> vehicles.add(new PlanningUnassignedVehicleResponse(tractor.getRegistration(),
            vehicleCostService.calculateDepreciationForPeriod(tractor.getPurchaseCost(),tractor.getDepreciationStartDate(),tractor.getDepreciationEndDate(),startDate,endDate))));
        unassignedSemiTrailers.forEach(semiTrailer -> vehicles.add(new PlanningUnassignedVehicleResponse(semiTrailer.getRegistration(), vehicleCostService
            .calculateDepreciationForPeriod(semiTrailer.getPurchaseCost(),semiTrailer.getDepreciationStartDate(),semiTrailer.getDepreciationEndDate(),startDate,endDate))));

        List<String> registrations = vehicles.stream().map(PlanningUnassignedVehicleResponse::registration).toList();
        BigDecimal depreciationCost = MoneyUtils.sum(vehicles, PlanningUnassignedVehicleResponse::depreciationCost);

        return new PlanningUnassignedVehiclesResponse(registrations, depreciationCost, vehicles);
    }

}