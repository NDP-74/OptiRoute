package com.optiroute.backend.service.transport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.dto.request.transport.RouteSegmentRequest;
import com.optiroute.backend.dto.request.transport.ServiceInfoRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.entity.event.Event;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.exception.TransportPlanningException;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.repository.EventRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.transport.ServiceRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.type.RouteTimeMode;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceRouteService {

    private final ServiceRepository serviceRepository;
    private final RouteRepository routeRepository;
    private final EventRepository eventRepository;
    private final TractorRepository tractorRepository;
    private final SemiTrailerRepository semiTrailerRepository;

    @Transactional
    public Service create(TransportRequest req, RoutesResponse response) {
        RouteDto fastestRoute = response.getRoutes().stream().min((route1, route2) -> Long.compare(route1.getDuration(),route2.getDuration()))
            .orElseThrow(() -> new IllegalArgumentException("No route available"));

        return create(req,fastestRoute);
    }

    @Transactional
    public Service create(TransportRequest req, RouteDto routeDto) {
        return create(req, List.of(new TransportRouteSegment(req, routeDto, false)));
    }

    @Transactional
    public Service create(TransportRequest req, List<TransportRouteSegment> segments) {
        Service service = new Service();
        applyService(service,req);
        service = serviceRepository.save(service);

        for (TransportRouteSegment segment : segments) {
            Route route = new Route();
            route.setService(service);
            applyRoute(route, segment);
            routeRepository.save(route);
        }

        return service;
    }

    @Transactional
    public Service update(Long id, TransportRequest req, RouteDto routeDto) {
        return update(id, req, List.of(new TransportRouteSegment(req, routeDto, false)));
    }

    // Replanification complète du service : les segments sont recalculés et les événements rattachés au nouveau segment correspondant
    @Transactional
    public Service update(Long id, TransportRequest req, List<TransportRouteSegment> segments) {
        Service service = serviceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Service not found with id " + id));
        List<Route> existingRoutes = routeRepository.findByServiceIdOrderByIdAsc(id);
        List<Event> events = existingRoutes.isEmpty() ? List.of()
            : eventRepository.findByRouteIdInOrderByRouteIdAscIdAsc(existingRoutes.stream().map(Route::getId).toList());

        applyService(service,req);
        service = serviceRepository.save(service);

        List<Route> plannedRoutes = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            Route route = index < existingRoutes.size() ? existingRoutes.get(index) : new Route();
            route.setService(service);
            applyRoute(route, segments.get(index));
            plannedRoutes.add(routeRepository.save(route));
        }
        reassignEvents(events, segments, plannedRoutes);
        if (existingRoutes.size() > segments.size()) {
            routeRepository.deleteAll(existingRoutes.subList(segments.size(), existingRoutes.size()));
        }

        return service;
    }

    // Modification isolée d'un segment : les autres segments du service ne sont pas modifiés
    @Transactional
    public Route updateRoute(Long serviceId, Long routeId, RouteSegmentRequest req) {
        Route route = routeRepository.findById(routeId).filter(candidate -> candidate.getService().getId().equals(serviceId))
            .orElseThrow(() -> new EntityNotFoundException("Route " + routeId + " not found for service " + serviceId));
        RouteDto routeDto = req.selectedRoute();

        route.setDriverId(req.driverId());
        route.setTractorId(req.tractorId());
        route.setSemiTrailerId(req.semiTrailerId());
        route.setEmptyTrip(req.emptyTrip());
        route.setWaypoints(userWaypoints(req.waypoints()));

        route.setOriginName(req.originName());
        route.setOriginAddress(req.originAddress());
        route.setOriginLat(req.originLat());
        route.setOriginLng(req.originLng());

        route.setDestinationName(req.destinationName());
        route.setDestinationAddress(req.destinationAddress());
        route.setDestinationLat(req.destinationLat());
        route.setDestinationLng(req.destinationLng());

        applyRouteDto(route, routeDto);
        if (req.timeMode() == RouteTimeMode.ARRIVAL) {
            route.setArrivalStart(req.routeTime());
            route.setDepartureStart(req.routeTime().minusSeconds(routeDto.getDuration()));
        } else {
            route.setDepartureStart(req.routeTime());
            route.setArrivalStart(req.routeTime().plusSeconds(routeDto.getDuration()));
        }
        route = routeRepository.save(route);

        List<Event> events = eventRepository.findByRouteIdOrderByIdAsc(route.getId());
        for (Event event : events) {
            attachEvent(event, route);
        }
        eventRepository.saveAll(events);

        return route;
    }

    // Mise à jour des informations du service sans toucher aux itinéraires ni aux horaires
    @Transactional
    public Service updateInfo(Long id, ServiceInfoRequest req) {
        Service service = serviceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Service not found with id " + id));
        if (req.name() != null && !req.name().isBlank()) {
            service.setName(req.name());
        }
        service.setCustomerId(req.customerId());
        service.setRevenue(req.revenue());
        service.setLoadingDurationMinutes(req.loadingDurationMinutes());
        service.setUnloadingDurationMinutes(req.unloadingDurationMinutes());

        return serviceRepository.save(service);
    }

    @Transactional
    public void delete(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new EntityNotFoundException("Service not found with id " + id);
        }

        // Les événements n'ont pas de sens sans le transport : ils sont supprimés avec lui
        eventRepository.deleteByServiceId(id);
        routeRepository.deleteByServiceId(id);
        serviceRepository.deleteById(id);
    }

    // Un événement suit le segment qui passe par son point d'intérêt ; sans point d'intérêt, celui du même jour, sinon le premier
    private void reassignEvents(List<Event> events, List<TransportRouteSegment> segments, List<Route> routes) {
        if (events.isEmpty()) {
            return;
        }

        Map<Long, Route> routeByEventId = new HashMap<>();
        for (int index = 0; index < segments.size(); index++) {
            List<Position> waypoints = segments.get(index).request().waypoints();
            if (waypoints == null) continue;
            for (Position waypoint : waypoints) {
                if (waypoint.getEventId() != null) {
                    routeByEventId.putIfAbsent(waypoint.getEventId(), routes.get(index));
                }
            }
        }

        for (Event event : events) {
            Route target = routeByEventId.get(event.getId());
            if (target == null) {
                target = routes.stream().filter(route -> sameDay(event, route.getDepartureStart())).findFirst().orElse(routes.getFirst());
            }
            event.setRoute(target);
            attachEvent(event, target);
        }
        eventRepository.saveAll(events);
    }

    private static boolean sameDay(Event event, OffsetDateTime departure) {
        return event.getEventDate() != null && departure != null
            && event.getEventDate().toLocalDate().equals(departure.toLocalDateTime().toLocalDate());
    }

    // Aligne la date et le véhicule de l'événement sur son segment (le véhicule du segment a pu changer)
    private void attachEvent(Event event, Route route) {
        if (route.getDepartureStart() != null) {
            event.setEventDate(route.getDepartureStart().toLocalDateTime());
        }
        if (event.getTractor() != null && !Objects.equals(event.getTractor().getId(), route.getTractorId())) {
            if (route.getTractorId() == null) {
                throw new TransportPlanningException("Le trajet n'a plus de tracteur alors qu'un événement y est rattaché.");
            }
            event.setTractor(tractorRepository.getReferenceById(route.getTractorId()));
        }
        if (event.getSemiTrailer() != null && !Objects.equals(event.getSemiTrailer().getId(), route.getSemiTrailerId())) {
            if (route.getSemiTrailerId() == null) {
                throw new TransportPlanningException("Le trajet n'a plus de semi-remorque alors qu'un événement y est rattaché. "
                    + "Supprimez cet événement ou conservez une semi-remorque.");
            }
            event.setSemiTrailer(semiTrailerRepository.getReferenceById(route.getSemiTrailerId()));
        }
    }

    private static List<Position> userWaypoints(List<Position> waypoints) {
        return waypoints == null ? new ArrayList<>()
            : new ArrayList<>(waypoints.stream().filter(waypoint -> waypoint.getEventId() == null).toList());
    }

    private void applyService(Service service, TransportRequest req) {
        service.setName(req.name());
        service.setCustomerId(req.customerId());
        service.setRevenue(req.revenue());
        if (req.externalId() != null) {
            service.setExternalId(req.externalId());
        }
        if (req.externalSource() != null) {
            service.setExternalSource(req.externalSource());
        }
        if (req.serviceTiming() != null) {
            var timing = req.serviceTiming();
            service.setLoadingDurationMinutes(timing.loadingDurationMinutes());
            service.setUnloadingDurationMinutes(timing.unloadingDurationMinutes());
            service.setDepartureStart(timing.departureStart());
            service.setDepartureEnd(timing.departureEnd());
            service.setArrivalStart(timing.arrivalStart());
            service.setArrivalEnd(timing.arrivalEnd());
        }
    }

    // Les coûts driver/vehicle/structure restent null pour l'instant
    private void applyRoute(Route route, TransportRouteSegment segment) {
        TransportRequest req = segment.request();
        RouteDto routeDto = segment.route();
        route.setDriverId(req.driverId());
        route.setTractorId(req.tractorId());
        route.setSemiTrailerId(req.semiTrailerId());

        applyRouteTimes(route, req, routeDto, segment.calculatedTimes());
        if (req.waypoints() != null) {
            route.setWaypoints(userWaypoints(req.waypoints()));
        }
        route.setEmptyTrip(req.emptyTrip());

        route.setOriginName(req.originName());
        route.setOriginAddress(req.originAddress());
        route.setOriginLat(req.originLat());
        route.setOriginLng(req.originLng());

        route.setDestinationName(req.destinationName());
        route.setDestinationAddress(req.destinationAddress());
        route.setDestinationLat(req.destinationLat());
        route.setDestinationLng(req.destinationLng());

        applyRouteDto(route, routeDto);
    }

    private static void applyRouteDto(Route route, RouteDto routeDto) {
        route.setDistanceMeters(routeDto.getDistanceMeters());
        route.setDurationSeconds(routeDto.getDuration());
        route.setPolyline(routeDto.getPolyline());
        if (routeDto.getType() != null) {
            route.setRouteType(routeDto.getType());
        }
        route.setFuelCost(routeDto.getCosts().getFuelCost());
        route.setTollCost(routeDto.getCosts().getTollCost());
    }

    private void applyRouteTimes(Route route, TransportRequest req, RouteDto routeDto, boolean calculatedTimes) {
        Service service = route.getService();
        if (!calculatedTimes && "DASHDOC".equalsIgnoreCase(service.getExternalSource())) {
            route.setDepartureStart(service.getDepartureStart());
            route.setArrivalStart(service.getArrivalStart());
            return;
        }

        if (req.routeTime() == null) {
            route.setDepartureStart(null);
            route.setArrivalStart(null);
            return;
        }

        if (req.timeMode() == RouteTimeMode.ARRIVAL) {
            route.setArrivalStart(req.routeTime());
            route.setDepartureStart(req.routeTime().minusSeconds(routeDto.getDuration()));
        } else {
            route.setDepartureStart(req.routeTime());
            route.setArrivalStart(req.routeTime().plusSeconds(routeDto.getDuration()));
        }
    }
}
