package com.optiroute.backend.service.transport;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.request.transport.RouteSegmentRequest;
import com.optiroute.backend.dto.request.transport.ServiceInfoRequest;
import com.optiroute.backend.dto.request.transport.TransportFromRouteRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.exception.TransportPlanningException;
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

    // Les appels HERE (calcul et planification) sont faits hors transaction ; seule l'écriture du service
    // et de ses trajets est transactionnelle (ServiceRouteService), sans sauvegarde partielle en cas d'échec.
    public Service createTransport(TransportRequest request) {
        RouteDto selectedRoute = calculateRoute(request);
        return serviceRouteService.create(request, transportRoutePlanner.plan(request, selectedRoute));
    }

    public Service updateTransport(Long id, TransportRequest request) {
        RouteDto selectedRoute = calculateRoute(request);
        return serviceRouteService.update(id, request, transportRoutePlanner.plan(request, selectedRoute));
    }

    private RouteDto calculateRoute(TransportRequest request) {
        RouteRequest routeRequest = routeRequestFactory.fromTransport(request);
        routeRequest.setDriverId(request.driverId());
        RoutesResponse routeResponse = routeOptimizationService.calculateRoute(routeRequest);

        return routeResponse.getRoutes().stream()
            .min((first, second) -> Long.compare(first.getDuration(), second.getDuration()))
            .orElseThrow(() -> new TransportPlanningException("Planning incompatible : aucun itinéraire disponible."));
    }

    public Service createFromRoute(TransportFromRouteRequest request) {
        return serviceRouteService.create(request.transport(),
            transportRoutePlanner.plan(request.transport(), request.selectedRoute()));
    }

    public Service updateFromRoute(Long id, TransportFromRouteRequest request) {
        return serviceRouteService.update(id, request.transport(),
            transportRoutePlanner.plan(request.transport(), request.selectedRoute()));
    }

    @Transactional
    public void updateRoute(Long serviceId, Long routeId, RouteSegmentRequest request) {
        serviceRouteService.updateRoute(serviceId, routeId, request);
    }

    @Transactional
    public void updateServiceInfo(Long id, ServiceInfoRequest request) {
        serviceRouteService.updateInfo(id, request);
    }

    @Transactional
    public void deleteTransport(Long id) {
        serviceRouteService.delete(id);
    }
}
