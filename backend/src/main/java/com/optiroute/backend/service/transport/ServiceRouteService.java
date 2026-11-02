package com.optiroute.backend.service.transport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.transport.ServiceRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceRouteService {

    private final ServiceRepository serviceRepository;
    private final RouteRepository routeRepository;

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

    @Transactional
    public Service update(Long id, TransportRequest req, List<TransportRouteSegment> segments) {
        Service service = serviceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Service not found with id " + id));
        applyService(service,req);
        service = serviceRepository.save(service);

        List<Route> existingRoutes = routeRepository.findByServiceIdOrderByIdAsc(id);
        for (int index = 0; index < segments.size(); index++) {
            Route route = index < existingRoutes.size() ? existingRoutes.get(index) : new Route();
            route.setService(service);
            applyRoute(route, segments.get(index));
            routeRepository.save(route);
        }
        if (existingRoutes.size() > segments.size()) {
            routeRepository.deleteAll(existingRoutes.subList(segments.size(), existingRoutes.size()));
        }

        return service;
    }

    @Transactional
    public void delete(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new EntityNotFoundException("Service not found with id " + id);
        }

        routeRepository.deleteByServiceId(id);
        serviceRepository.deleteById(id);
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
            route.setWaypoints(new ArrayList<>(req.waypoints()));
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

        route.setDistanceMeters(routeDto.getDistanceMeters());
        route.setDurationSeconds(routeDto.getDuration());
        route.setPolyline(routeDto.getPolyline());
        if (routeDto.getType() != null) {
            route.setRouteType(routeDto.getType());
        }
        route.setFuelCost(BigDecimal.valueOf(routeDto.getCosts().getFuelCost()));
        route.setTollCost(BigDecimal.valueOf(routeDto.getCosts().getTollCost()));
    }

    private void applyRouteTimes(Route route, TransportRequest req, RouteDto routeDto, boolean calculatedTimes) {
        Service service = route.getService();
        if (!calculatedTimes && "DASHDOC".equalsIgnoreCase(service.getExternalSource())) {
            route.setDepartureStart(service.getDepartureStart());
            route.setDepartureEnd(service.getDepartureEnd());
            route.setArrivalStart(service.getArrivalStart());
            route.setArrivalEnd(service.getArrivalEnd());
            return;
        }

        route.setDepartureEnd(null);
        route.setArrivalEnd(null);
        if (req.routeTime() == null) {
            route.setDepartureStart(null);
            route.setArrivalStart(null);
            return;
        }

        if (req.timeMode() == com.optiroute.backend.type.RouteTimeMode.ARRIVAL) {
            route.setArrivalStart(req.routeTime());
            route.setDepartureStart(req.routeTime().minusSeconds(routeDto.getDuration()));
        } else {
            route.setDepartureStart(req.routeTime());
            route.setArrivalStart(req.routeTime().plusSeconds(routeDto.getDuration()));
        }
    }
}
