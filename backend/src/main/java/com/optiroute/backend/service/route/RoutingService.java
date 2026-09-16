package com.optiroute.backend.service.route;

import java.util.List;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.optiroute.backend.client.PtvApiClient;
import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.model.TruckConfiguration;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.utils.CommonUtils;

@Service
public class RoutingService {

    private final PtvApiClient ptvApiClient;

    public RoutingService(PtvApiClient ptvApiClient) {
        this.ptvApiClient = ptvApiClient;
    }

    public String calculateRoutes(RouteRequest request, TruckConfiguration truckConfiguration) {

        List<String> waypoints = new java.util.ArrayList<>();
        waypoints.add(request.getOrigin().getLat() + "," + request.getOrigin().getLng());
        if (request.getWaypoints() != null) {
            request.getWaypoints().stream().filter(point -> point != null).forEach(point -> waypoints.add(point.getLat() + "," + point.getLng()));
        }
        waypoints.add(request.getDestination().getLat() + "," + request.getDestination().getLng());

        String routeTime = request.getRouteTime() == null ? CommonUtils.formatTime(OffsetDateTime.now()) : CommonUtils.formatTime(request.getRouteTime());
        RouteTimeMode timeMode = request.getTimeMode() == null ? RouteTimeMode.DEPARTURE : request.getTimeMode();

        return ptvApiClient.getRoutes(waypoints,truckConfiguration,routeTime,timeMode,request.getMode());
    }
}