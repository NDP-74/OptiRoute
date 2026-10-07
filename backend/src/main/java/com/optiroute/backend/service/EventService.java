package com.optiroute.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.dto.request.EventRequest;
import com.optiroute.backend.dto.response.EventResponse;
import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.entity.event.Event;
import com.optiroute.backend.entity.event.PointOfInterest;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.EventRepository;
import com.optiroute.backend.repository.PointOfInterestRepository;
import com.optiroute.backend.repository.cost.CostParameterRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.service.route.RouteOptimizationService;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.type.cost.CostParameterAssignmentType;

import jakarta.persistence.EntityNotFoundException;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final RouteRepository routeRepository;
    private final CostParameterRepository costParameterRepository;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final TractorRepository tractorRepository;
    private final SemiTrailerRepository semiTrailerRepository;
    private final RouteOptimizationService routeOptimizationService;

    public EventService(EventRepository eventRepository, RouteRepository routeRepository, CostParameterRepository costParameterRepository,
            PointOfInterestRepository pointOfInterestRepository, TractorRepository tractorRepository, SemiTrailerRepository semiTrailerRepository,
            RouteOptimizationService routeOptimizationService) {
        this.eventRepository = eventRepository;
        this.routeRepository = routeRepository;
        this.costParameterRepository = costParameterRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.tractorRepository = tractorRepository;
        this.semiTrailerRepository = semiTrailerRepository;
        this.routeOptimizationService = routeOptimizationService;
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        if ((request.tractorId() == null) == (request.semiTrailerId() == null)) {
            throw new IllegalArgumentException("Exactly one of tractor or semi-trailer must be provided");
        }

        Route route = routeRepository.findFirstByServiceId(request.serviceId()).orElseThrow(() -> new EntityNotFoundException("Route not found for service " + request.serviceId()));

        CostParameter costParameter = costParameterRepository.findById(request.costParameterId())
                .orElseThrow(() -> new EntityNotFoundException("Cost parameter not found with id " + request.costParameterId()));

        if (costParameter.getAssignmentType() != CostParameterAssignmentType.MANUAL) {
            throw new IllegalArgumentException("Cost parameter is not manually assignable");
        }

        PointOfInterest pointOfInterest = request.pointOfInterestId() == null ? null
                : pointOfInterestRepository.findById(request.pointOfInterestId()).orElseThrow(() -> new EntityNotFoundException("Point of interest not found with id " + request.pointOfInterestId()));

        Tractor tractor = null;
        SemiTrailer semiTrailer = null;

        if (request.tractorId() != null) {
            if (!request.tractorId().equals(route.getTractorId())) {
                throw new IllegalArgumentException("Tractor does not belong to this route");
            }
            tractor = tractorRepository.findById(request.tractorId()).orElseThrow(() -> new EntityNotFoundException("Tractor not found with id " + request.tractorId()));
        } else {
            if (!request.semiTrailerId().equals(route.getSemiTrailerId())) {
                throw new IllegalArgumentException("Semi-trailer does not belong to this route");
            }
            semiTrailer = semiTrailerRepository.findById(request.semiTrailerId()).orElseThrow(() -> new EntityNotFoundException("Semi-trailer not found with id " + request.semiTrailerId()));
        }

        LocalDateTime eventDate = route.getStartDate() == null ? LocalDateTime.now() : route.getStartDate().toLocalDateTime();

        Event event = Event.builder().route(route).costParameter(costParameter).pointOfInterest(pointOfInterest).tractor(tractor).semiTrailer(semiTrailer).eventDate(eventDate).build();

        Event saved = eventRepository.save(event);

        if (pointOfInterest != null) {
            recalculateRoute(route);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getByService(Long serviceId) {
        return routeRepository.findFirstByServiceId(serviceId).map(route -> eventRepository.findByRouteIdOrderByIdAsc(route.getId()).stream().map(this::toResponse).toList()).orElse(List.of());
    }

    private EventResponse toResponse(Event event) {
        PointOfInterest poi = event.getPointOfInterest();
        Tractor tractor = event.getTractor();
        SemiTrailer semiTrailer = event.getSemiTrailer();

        return new EventResponse(event.getId(), event.getRoute().getId(), event.getCostParameter().getId(), event.getCostParameter().getLabel(), poi == null ? null : poi.getId(),
                poi == null ? null : poi.getLabel(), tractor == null ? null : tractor.getId(), semiTrailer == null ? null : semiTrailer.getId(), event.getEventDate());
    }

    // Recalcule l'itinéraire avec les points d'intérêt de tous les événements de la route comme étapes
    private void recalculateRoute(Route route) {
        List<Position> waypoints = eventRepository.findByRouteIdOrderByIdAsc(route.getId()).stream().map(Event::getPointOfInterest)
                .filter(poi -> poi != null && poi.getLatitude() != null && poi.getLongitude() != null)
                .map(poi -> new Position(poi.getLatitude().doubleValue(), poi.getLongitude().doubleValue())).toList();

        GpsModeType mode = route.getRouteType() == null ? GpsModeType.FASTEST : route.getRouteType();

        RouteRequest routeRequest = new RouteRequest();
        routeRequest.setOrigin(new Position(route.getOriginLat(), route.getOriginLng()));
        routeRequest.setDestination(new Position(route.getDestinationLat(), route.getDestinationLng()));
        routeRequest.setWaypoints(waypoints);
        routeRequest.setMode(mode);
        routeRequest.setRouteTime(route.getStartDate());
        routeRequest.setTimeMode(RouteTimeMode.DEPARTURE);
        routeRequest.setTractorId(route.getTractorId());
        routeRequest.setSemiTrailerId(route.getSemiTrailerId());
        routeRequest.setDriverId(route.getDriverId());
        routeRequest.setEmptyTrip(route.isEmptyTrip());

        List<RouteDto> routes = routeOptimizationService.calculateRoute(routeRequest).getRoutes();

        RouteDto selected = routes.stream().filter(candidate -> candidate.getType() == mode).findFirst()
                .orElseGet(() -> routes.stream().min(Comparator.comparingLong(RouteDto::getDuration)).orElseThrow(() -> new IllegalStateException("No route available")));

        route.setDistanceMeters(selected.getDistanceMeters());
        route.setDurationSeconds(selected.getDuration());
        route.setPolyline(selected.getPolyline());
        route.setRouteType(selected.getType());
        route.setFuelCost(BigDecimal.valueOf(selected.getCosts().getFuelCost()));
        route.setTollCost(BigDecimal.valueOf(selected.getCosts().getTollCost()));

        if (route.getStartDate() != null) {
            route.setEndDate(route.getStartDate().plusSeconds(selected.getDuration()));
        }

        routeRepository.save(route);
    }}
