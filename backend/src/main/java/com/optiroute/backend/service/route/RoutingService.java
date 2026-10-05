package com.optiroute.backend.service.route;

import java.util.List;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.optiroute.backend.client.HereApiClient;
import com.optiroute.backend.client.PtvApiClient;
import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.model.TruckConfiguration;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.utils.CommonUtils;

@Service
public class RoutingService {

    private final PtvApiClient ptvApiClient;
    private final HereApiClient hereApiClient;
    private final String provider;

    public RoutingService(PtvApiClient ptvApiClient, HereApiClient hereApiClient, @Value("${routing.provider:here}") String provider) {
        this.ptvApiClient = ptvApiClient;
        this.hereApiClient = hereApiClient;
        this.provider = provider;
    }

    public boolean isHere() {
        return "here".equalsIgnoreCase(provider);
    }

    public String calculateRoutes(RouteRequest request, TruckConfiguration truckConfiguration, double driverHourlyRate) {

        List<String> waypoints = new java.util.ArrayList<>();
        waypoints.add(request.getOrigin().getLat() + "," + request.getOrigin().getLng());
        if (request.getWaypoints() != null) {
            request.getWaypoints().stream().filter(point -> point != null).forEach(point -> waypoints.add(point.getLat() + "," + point.getLng()));
        }
        waypoints.add(request.getDestination().getLat() + "," + request.getDestination().getLng());

        String routeTime = request.getRouteTime() == null ? CommonUtils.formatTime(OffsetDateTime.now()) : CommonUtils.formatTime(request.getRouteTime());
        RouteTimeMode timeMode = request.getTimeMode() == null ? RouteTimeMode.DEPARTURE : request.getTimeMode();

        if (isHere()) {
            List<String> viaPoints = waypoints.subList(1,waypoints.size() - 1);
            return hereApiClient.getRoutes(waypoints.getFirst(),waypoints.getLast(),viaPoints,routeTime,truckConfiguration,false,3);
        }

        return ptvApiClient.getRoutes(waypoints,truckConfiguration,routeTime,timeMode,request.getMode(),driverHourlyRate);
    }
}