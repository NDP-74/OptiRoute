package com.optiroute.backend.service.transport;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.request.transport.TransportFromRouteRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
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

    @Transactional
    public Service createTransport(TransportRequest request) {
        RouteRequest routeRequest = routeRequestFactory.fromTransport(request);
        RoutesResponse routeResponse = routeOptimizationService.calculateRoute(routeRequest);

        return serviceRouteService.create(request,routeResponse);
    }

    @Transactional
    public Service createFromRoute(TransportFromRouteRequest request) {
        return serviceRouteService.create(request.transport(),request.selectedRoute());
    }

    @Transactional
    public Service updateFromRoute(Long id, TransportFromRouteRequest request) {
        return serviceRouteService.update(id,request.transport(),request.selectedRoute());
    }

    @Transactional
    public void deleteTransport(Long id) {
        serviceRouteService.delete(id);
    }
}
