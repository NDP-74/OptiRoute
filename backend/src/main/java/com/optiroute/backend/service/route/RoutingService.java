package com.optiroute.backend.service.route;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.optiroute.backend.client.HereApiClient;
import com.optiroute.backend.client.PtvApiClient;
import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.model.TruckConfiguration;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.utils.CommonUtils;

@Service
public class RoutingService {

    private final PtvApiClient ptvApiClient;
    private final HereApiClient hereApiClient;
    private final RoutePtvParser routePtvParser;
    private final RouteHereParser routeHereParser;
    private final String provider;

    public RoutingService(PtvApiClient ptvApiClient, HereApiClient hereApiClient, RoutePtvParser routePtvParser, RouteHereParser routeHereParser, @Value("${routing.provider:here}") String provider) {
        this.ptvApiClient = ptvApiClient;
        this.hereApiClient = hereApiClient;
        this.routePtvParser = routePtvParser;
        this.routeHereParser = routeHereParser;
        this.provider = provider;
    }

    public boolean isHere() {
        return "here".equalsIgnoreCase(provider);
    }

    public Map<GpsModeType, RoutePtvParser.ParsedRoute> calculateRoutes(RouteRequest request, TruckConfiguration truckConfiguration, double driverHourlyRate) {

        List<String> waypoints = new java.util.ArrayList<>();
        waypoints.add(request.getOrigin().getLat() + "," + request.getOrigin().getLng());
        if (request.getWaypoints() != null) {
            request.getWaypoints().stream().filter(point -> point != null).forEach(point -> waypoints.add(point.getLat() + "," + point.getLng()));
        }
        waypoints.add(request.getDestination().getLat() + "," + request.getDestination().getLng());

        String routeTime = request.getRouteTime() == null ? CommonUtils.formatTime(OffsetDateTime.now()) : CommonUtils.formatTime(request.getRouteTime());
        RouteTimeMode timeMode = request.getTimeMode() == null ? RouteTimeMode.DEPARTURE : request.getTimeMode();

        Map<GpsModeType, RoutePtvParser.ParsedRoute> routes = new LinkedHashMap<>();
        for (GpsModeType mode : GpsModeType.values()) {
            RoutePtvParser.ParsedRoute parsed = isHere() ? fetchHere(waypoints,routeTime,truckConfiguration,mode) : fetchPtv(waypoints,truckConfiguration,routeTime,timeMode,mode,driverHourlyRate);
            if (parsed != null) {
                routes.put(mode,parsed);
            }
        }
        return routes;
    }

    private RoutePtvParser.ParsedRoute fetchHere(List<String> waypoints, String routeTime, TruckConfiguration truckConfiguration, GpsModeType mode) {
        List<String> viaPoints = waypoints.subList(1,waypoints.size() - 1);
        String raw = hereApiClient.getRoutes(waypoints.getFirst(),waypoints.getLast(),viaPoints,routeTime,truckConfiguration,GpsModeType.CHEAPEST.equals(mode),0);
        return routeHereParser.parseRoutes(raw).stream().findFirst()
            .map(here -> new RoutePtvParser.ParsedRoute(here.duration, here.baseDuration, here.distanceMeters, here.polyline, here.tollCost, here.rawJson)).orElse(null);
    }

    private RoutePtvParser.ParsedRoute fetchPtv(List<String> waypoints, TruckConfiguration truckConfiguration, String routeTime, RouteTimeMode timeMode, GpsModeType mode,
        double driverHourlyRate) {
        String raw = ptvApiClient.getRoutes(waypoints,truckConfiguration,routeTime,timeMode,mode,driverHourlyRate);
        return routePtvParser.parseRoutes(raw).stream().findFirst().orElse(null);
    }
}