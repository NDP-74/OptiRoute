package com.optiroute.backend.service.transport;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

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
import com.optiroute.backend.service.cost.CostParameterSnapshot;

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
        if (route.getDepartureStart() == null || route.getArrivalStart() == null) {
            throw new IllegalStateException("Route departure and arrival times must be calculated before costs");
        }
        LocalDate date = route.getDepartureStart().atZoneSameInstant(ZONE).toLocalDate();

        Driver driver = route.getDriverId() == null ? null : driverRepository.findById(route.getDriverId()).orElse(null);
        Map<LocalDate, List<Route>> routesByDate = new HashMap<>();
        for (int offset = -1; offset <= 1; offset++) {
            LocalDate day = date.plusDays(offset);
            routesByDate.put(day,findDayRoutes(route,day));
        }
        List<Route> dayRoutes = routesByDate.get(date);
        List<CostCalculationContext> dailyContexts = new ArrayList<>();
        for (int offset = -1; offset <= 0; offset++) {
            LocalDate day = date.plusDays(offset);
            List<Route> routes = routesByDate.get(day);
            if (routes.isEmpty()) {
                continue;
            }
            int nextDayCount = routesByDate.get(day.plusDays(1)).size();
            dailyContexts.addAll(buildDailyContexts(routes,driver,routes.size() + nextDayCount));
        }
        CostCalculationContext context = dailyContexts.stream().filter(c -> c.date().equals(date)).toList()
            .get(dayRoutes.indexOf(route));

        Tractor tractor = route.getTractorId() == null ? null : tractorRepository.findById(route.getTractorId()).orElse(null);
        SemiTrailer semiTrailer = route.getSemiTrailerId() == null ? null : semiTrailerRepository.findById(route.getSemiTrailerId()).orElse(null);

        double fuelCost = route.getFuelCost() == null ? 0 : route.getFuelCost().doubleValue();
        double tollCost = route.getTollCost() == null ? 0 : route.getTollCost().doubleValue();

        CostCategoryResponse vehicle = vehicleCostService.calculateCosts(tractor,semiTrailer,context,fuelCost,tollCost,dailyContexts);
        CostCategoryResponse driverCosts = driver == null ? new CostCategoryResponse(List.of(), 0) : driverCostService.calculateCosts(driver,context,dailyContexts);
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

    public List<TransportCostDetailsResponse> calculateAll(List<Route> requestedRoutes) {
        if (requestedRoutes.isEmpty()) {
            return List.of();
        }

        List<LocalDate> requestedDates = requestedRoutes.stream()
            .map(route -> {
                if (route.getDepartureStart() == null || route.getArrivalStart() == null) {
                    throw new IllegalStateException("Route departure and arrival times must be calculated before costs");
                }
                return route.getDepartureStart().atZoneSameInstant(ZONE).toLocalDate();
            })
            .toList();
        LocalDate firstDate = requestedDates.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate lastDate = requestedDates.stream().max(Comparator.naturalOrder()).orElseThrow();

        List<Long> driverIds = requestedRoutes.stream().map(Route::getDriverId).filter(Objects::nonNull).distinct().toList();
        List<Route> contextRoutes = driverIds.isEmpty()
            ? List.of()
            : routeRepository.findByDriverIdInAndDepartureStartGreaterThanEqualAndDepartureStartLessThan(
                driverIds,
                firstDate.minusDays(1).atStartOfDay(ZONE).toOffsetDateTime(),
                lastDate.plusDays(2).atStartOfDay(ZONE).toOffsetDateTime()
            );

        Map<Long, Route> allRoutesById = new LinkedHashMap<>();
        contextRoutes.forEach(route -> allRoutesById.put(route.getId(), route));
        requestedRoutes.forEach(route -> allRoutesById.put(route.getId(), route));
        List<Route> allRoutes = new ArrayList<>(allRoutesById.values());

        List<Long> routeIds = allRoutes.stream().map(Route::getId).filter(Objects::nonNull).distinct().toList();
        Map<Long, List<Event>> eventsByRoute = routeIds.isEmpty()
            ? Map.of()
            : eventRepository.findByRouteIdInOrderByRouteIdAscIdAsc(routeIds).stream()
                .filter(event -> event.getRoute() != null)
                .collect(Collectors.groupingBy(event -> event.getRoute().getId()));

        Map<Long, Driver> driversById = driverRepository.findAllById(driverIds).stream()
            .collect(Collectors.toMap(Driver::getId, driver -> driver, (first, ignored) -> first));
        List<Long> tractorIds = allRoutes.stream().map(Route::getTractorId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Tractor> tractorsById = tractorRepository.findAllById(tractorIds).stream()
            .collect(Collectors.toMap(Tractor::getId, tractor -> tractor, (first, ignored) -> first));
        List<Long> semiTrailerIds = allRoutes.stream().map(Route::getSemiTrailerId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SemiTrailer> semiTrailersById = semiTrailerIds.isEmpty()
            ? Map.of()
            : semiTrailerRepository.findWithTrailerTypeByIdIn(semiTrailerIds).stream()
                .collect(Collectors.toMap(SemiTrailer::getId, semiTrailer -> semiTrailer, (first, ignored) -> first));
        CostParameterSnapshot costParameters = costParameterEngine.loadSnapshot();

        Map<DriverDayKey, List<Route>> routesByDriverDay = new HashMap<>();
        for (Route route : allRoutes) {
            if (route.getDepartureStart() == null) {
                continue;
            }
            LocalDate date = route.getDepartureStart().atZoneSameInstant(ZONE).toLocalDate();
            DriverDayKey key = driverDayKey(route, date);
            List<Route> dayRoutes = routesByDriverDay.computeIfAbsent(key, ignored -> new ArrayList<>());
            if (dayRoutes.stream().noneMatch(existing -> Objects.equals(existing.getId(), route.getId()))) {
                dayRoutes.add(route);
            }
        }

        Map<DriverDayKey, List<CostCalculationContext>> contextsByDriverDay = new HashMap<>();
        Map<Long, CostCalculationContext> contextByRouteId = new HashMap<>();
        for (Map.Entry<DriverDayKey, List<Route>> entry : routesByDriverDay.entrySet()) {
            DriverDayKey key = entry.getKey();
            DriverDayKey nextDayKey = new DriverDayKey(key.driverId(), key.isolatedRouteId(), key.date().plusDays(1));
            int nightTransportCount = entry.getValue().size() + routesByDriverDay.getOrDefault(nextDayKey, List.of()).size();
            Driver driver = key.driverId() == null ? null : driversById.get(key.driverId());
            List<CostCalculationContext> contexts = buildDailyContexts(
                entry.getValue(), driver, nightTransportCount, eventsByRoute, semiTrailersById
            );
            contextsByDriverDay.put(key, contexts);
            for (int index = 0; index < entry.getValue().size(); index++) {
                Route route = entry.getValue().get(index);
                if (route.getId() != null) {
                    contextByRouteId.put(route.getId(), contexts.get(index));
                }
            }
        }

        List<TransportCostDetailsResponse> results = new ArrayList<>(requestedRoutes.size());
        for (int index = 0; index < requestedRoutes.size(); index++) {
            Route route = requestedRoutes.get(index);
            LocalDate date = requestedDates.get(index);
            DriverDayKey dayKey = driverDayKey(route, date);
            CostCalculationContext context = contextByRouteId.get(route.getId());
            if (context == null) {
                throw new IllegalStateException("Unable to build a cost context for route " + route.getId());
            }

            List<CostCalculationContext> dailyContexts = new ArrayList<>();
            dailyContexts.addAll(contextsByDriverDay.getOrDefault(
                new DriverDayKey(dayKey.driverId(), dayKey.isolatedRouteId(), date.minusDays(1)), List.of()
            ));
            dailyContexts.addAll(contextsByDriverDay.getOrDefault(dayKey, List.of()));

            Driver driver = route.getDriverId() == null ? null : driversById.get(route.getDriverId());
            Tractor tractor = route.getTractorId() == null ? null : tractorsById.get(route.getTractorId());
            SemiTrailer semiTrailer = route.getSemiTrailerId() == null ? null : semiTrailersById.get(route.getSemiTrailerId());
            double fuelCost = route.getFuelCost() == null ? 0 : route.getFuelCost().doubleValue();
            double tollCost = route.getTollCost() == null ? 0 : route.getTollCost().doubleValue();

            CostCategoryResponse vehicle = vehicleCostService.calculateCosts(
                tractor, semiTrailer, context, fuelCost, tollCost, dailyContexts, costParameters
            );
            CostCategoryResponse driverCosts = driver == null
                ? new CostCategoryResponse(List.of(), 0)
                : driverCostService.calculateCosts(driver, context, dailyContexts, costParameters);
            CostCategoryResponse structure = structureCostService.calculateCosts(context, dailyContexts, costParameters);

            for (Event event : eventsByRoute.getOrDefault(route.getId(), List.of())) {
                AppliedCostResponse manualCost = costParameterEngine.calculateManualCost(event.getCostParameter(), context);
                if (manualCost == null) {
                    continue;
                }
                CostParameterCategoryType category = event.getCostParameter().getCategory();
                if (category == CostParameterCategoryType.VEHICLE) {
                    vehicle = withCost(vehicle, manualCost);
                } else if (category == CostParameterCategoryType.DRIVER) {
                    driverCosts = withCost(driverCosts, manualCost);
                } else {
                    structure = withCost(structure, manualCost);
                }
            }

            results.add(new TransportCostDetailsResponse(
                vehicle, driverCosts, structure, vehicle.totalCost() + driverCosts.totalCost() + structure.totalCost()
            ));
        }
        return results;
    }

    private List<CostCalculationContext> buildDailyContexts(List<Route> dayRoutes, Driver driver, int nightTransportCount) {
        Boolean driverEndsDayAtParking = endsDayAtParking(driver,dayRoutes);
        Map<Long, Double> driverWorkDurations = new HashMap<>();
        for (Route dayRoute : dayRoutes) {
            driverWorkDurations.put(dayRoute.getId(),calculateDriverWorkDurationHours(dayRoute));
        }
        double dailyDriverWorkDurationHours = driverWorkDurations.values().stream().mapToDouble(Double::doubleValue).sum();
        return dayRoutes.stream()
            .map(r -> buildContext(r,dayRoutes,driverWorkDurations,dailyDriverWorkDurationHours,driverEndsDayAtParking,nightTransportCount)).toList();
    }

    private List<CostCalculationContext> buildDailyContexts(
        List<Route> dayRoutes,
        Driver driver,
        int nightTransportCount,
        Map<Long, List<Event>> eventsByRoute,
        Map<Long, SemiTrailer> semiTrailersById
    ) {
        Boolean driverEndsDayAtParking = endsDayAtParking(driver, dayRoutes);
        Map<Long, Double> driverWorkDurations = new HashMap<>();
        for (Route dayRoute : dayRoutes) {
            driverWorkDurations.put(dayRoute.getId(), calculateDriverWorkDurationHours(
                dayRoute, eventsByRoute.getOrDefault(dayRoute.getId(), List.of())
            ));
        }
        double dailyDriverWorkDurationHours = driverWorkDurations.values().stream().mapToDouble(Double::doubleValue).sum();
        return dayRoutes.stream()
            .map(route -> buildContext(
                route, dayRoutes, driverWorkDurations, dailyDriverWorkDurationHours, driverEndsDayAtParking,
                nightTransportCount, semiTrailersById
            ))
            .toList();
    }

    private CostCategoryResponse withCost(CostCategoryResponse category, AppliedCostResponse cost) {
        List<AppliedCostResponse> costs = new ArrayList<>(category.costs());
        costs.add(cost);
        return new CostCategoryResponse(costs, category.totalCost() + cost.amount());
    }

    // Trajets du même jour pour le même conducteur (le véhicule suit le conducteur)
    private List<Route> findDayRoutes(Route route, LocalDate date) {
        if (route.getDriverId() == null) {
            return date.equals(route.getDepartureStart().atZoneSameInstant(ZONE).toLocalDate()) ? List.of(route) : List.of();
        }

        OffsetDateTime start = date.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
        List<Route> routes = new ArrayList<>(routeRepository.findByDriverIdAndDepartureStartGreaterThanEqualAndDepartureStartLessThan(route.getDriverId(),start,end));
        if (date.equals(route.getDepartureStart().atZoneSameInstant(ZONE).toLocalDate())) {
            routes.removeIf(r -> Objects.equals(r.getId(),route.getId()));
            routes.add(route);
        }
        return routes;
    }

    private Boolean endsDayAtParking(Driver driver, List<Route> dayRoutes) {
        if (driver == null || driver.getLocationLatitude() == null || driver.getLocationLongitude() == null) {
            return null;
        }
        Route lastRoute = dayRoutes.stream().max(Comparator.comparing(r -> r.getArrivalStart().toInstant())).orElseThrow();
        return lastRoute.getDestinationLat() == driver.getLocationLatitude().doubleValue()
            && lastRoute.getDestinationLng() == driver.getLocationLongitude().doubleValue();
    }

    private CostCalculationContext buildContext(Route route, List<Route> dayRoutes, Map<Long, Double> driverWorkDurations, double dailyDriverWorkDurationHours,
        Boolean driverEndsDayAtParking, int nightTransportCount) {
        return buildContext(route, dayRoutes, driverWorkDurations, dailyDriverWorkDurationHours, driverEndsDayAtParking,
            nightTransportCount, Map.of());
    }

    private CostCalculationContext buildContext(Route route, List<Route> dayRoutes, Map<Long, Double> driverWorkDurations,
        double dailyDriverWorkDurationHours, Boolean driverEndsDayAtParking, int nightTransportCount,
        Map<Long, SemiTrailer> semiTrailersById) {
        double dailyKm = dayRoutes.stream().mapToDouble(r -> r.getDistanceMeters() / 1000.0).sum();
        double dailyHours = dayRoutes.stream().mapToDouble(r -> r.getDurationSeconds() / 3600.0).sum();

        LocalTime dayStart = dayRoutes.stream().map(r -> r.getDepartureStart().atZoneSameInstant(ZONE).toLocalTime()).min(Comparator.naturalOrder()).orElse(null);
        LocalTime dayEnd = dayRoutes.stream().map(r -> r.getArrivalStart().atZoneSameInstant(ZONE).toLocalTime()).max(Comparator.naturalOrder()).orElse(null);

        Long trailerTypeId = null;
        if (route.getSemiTrailerId() != null) {
            SemiTrailer semiTrailer = semiTrailersById.isEmpty()
                ? semiTrailerRepository.findById(route.getSemiTrailerId()).orElse(null)
                : semiTrailersById.get(route.getSemiTrailerId());
            trailerTypeId = semiTrailer == null || semiTrailer.getTrailerType() == null ? null : semiTrailer.getTrailerType().getId();
        }

        return new CostCalculationContext(route.getDepartureStart().atZoneSameInstant(ZONE).toLocalDate(), route.getDistanceMeters() / 1000.0, dailyKm,
            route.getDurationSeconds() / 3600.0, dayRoutes.size(), route.isEmptyTrip(), route.getDepartureStart().atZoneSameInstant(ZONE).toLocalTime(),
            route.getArrivalStart().atZoneSameInstant(ZONE).toLocalTime(), dayStart, dayEnd, trailerTypeId, dailyDriverWorkDurationHours,
            driverWorkDurations.getOrDefault(route.getId(),route.getDurationSeconds() / 3600.0), driverEndsDayAtParking, nightTransportCount);
    }

    private double calculateDriverWorkDurationHours(Route route) {
        List<Event> events = route.getId() == null ? List.of() : eventRepository.findByRouteIdOrderByIdAsc(route.getId());
        return calculateDriverWorkDurationHours(route, events);
    }

    private double calculateDriverWorkDurationHours(Route route, List<Event> events) {
        double durationHours = route.getDurationSeconds() / 3600.0;
        var service = route.getService();
        if (service != null) {
            durationHours += toHours(service.getLoadingDurationMinutes()) + toHours(service.getUnloadingDurationMinutes());
        }
        durationHours += events.stream().map(Event::getDurationMinutes).filter(minutes -> minutes != null)
            .mapToInt(Integer::intValue).sum() / 60.0;
        return durationHours;
    }

    private DriverDayKey driverDayKey(Route route, LocalDate date) {
        return new DriverDayKey(route.getDriverId(), route.getDriverId() == null ? route.getId() : null, date);
    }

    private double toHours(Integer minutes) {
        return minutes == null ? 0 : minutes / 60.0;
    }

    private record DriverDayKey(Long driverId, Long isolatedRouteId, LocalDate date) {
    }
}
