package com.optiroute.backend.service.transport;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.request.transport.TransportFromRouteRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.mapper.RouteRequestFactory;
import com.optiroute.backend.service.route.RouteOptimizationService;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class TransportFacadeService {

    private final ServiceRouteService serviceRouteService;

    private final RouteRequestFactory routeRequestFactory;
    private final RouteOptimizationService routeOptimizationService;
    private final TransportRoutePlanner transportRoutePlanner;

    @Transactional
    public Service createTransport(TransportRequest request) {
        RouteRequest routeRequest = routeRequestFactory.fromTransport(request);
        routeRequest.setDriverId(request.driverId());
        RoutesResponse routeResponse = routeOptimizationService.calculateRoute(routeRequest);

        RouteDto selectedRoute = routeResponse.getRoutes().stream()
            .min((first, second) -> Long.compare(first.getDuration(), second.getDuration()))
            .orElseThrow(() -> new IllegalArgumentException("No route available"));
        return serviceRouteService.create(request, transportRoutePlanner.plan(request, selectedRoute));
    }

    @Transactional
    public Service createFromRoute(TransportFromRouteRequest request) {
        return serviceRouteService.create(request.transport(),
            transportRoutePlanner.plan(request.transport(), request.selectedRoute()));
    }

    @Transactional
    public Service updateFromRoute(Long id, TransportFromRouteRequest request) {
        return serviceRouteService.update(id, request.transport(),
            transportRoutePlanner.plan(request.transport(), request.selectedRoute()));
    }

    @Transactional
    public void deleteTransport(Long id) {
        serviceRouteService.delete(id);
    }
}
