package com.optiroute.backend.service.transport;

import java.math.BigDecimal;

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
        Service service = new Service();
        applyService(service,req);
        service = serviceRepository.save(service);

        Route route = new Route();
        route.setService(service);
        applyRoute(route,req,routeDto);
        routeRepository.save(route);

        return service;
    }

    @Transactional
    public Service update(Long id, TransportRequest req, RouteDto routeDto) {
        Service service = serviceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Service not found with id " + id));
        applyService(service,req);
        service = serviceRepository.save(service);

        Route route = routeRepository.findFirstByServiceId(id).orElseGet(Route::new);
        route.setService(service);
        applyRoute(route,req,routeDto);
        routeRepository.save(route);

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
    }

    // Les coûts driver/vehicle/structure restent null pour l'instant
    private void applyRoute(Route route, TransportRequest req, RouteDto routeDto) {
        route.setDriverId(req.driverId());
        route.setTractorId(req.tractorId());
        route.setSemiTrailerId(req.semiTrailerId());

        route.setStartDate(req.plannedStart());
        route.setEndDate(req.plannedEnd());
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
        route.setFuelCost(BigDecimal.valueOf(routeDto.getCosts().getFuelCost()));
        route.setTollCost(BigDecimal.valueOf(routeDto.getCosts().getTollCost()));
    }
}
