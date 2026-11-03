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
import com.optiroute.backend.model.RouteTimeline;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.utils.CommonUtils;
import com.optiroute.backend.exception.TransportPlanningException;

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

    public RouteTimeline calculateTimeline(RouteRequest request, TruckConfiguration truckConfiguration) {
        if (!isHere()) {
            throw new TransportPlanningException("Le découpage à 20 h nécessite actuellement le fournisseur HERE.");
        }
        List<String> via = request.getWaypoints() == null ? List.of() : request.getWaypoints().stream()
            .map(point -> point.getLat() + "," + point.getLng()).toList();
        String raw = hereApiClient.getRoutes(
            request.getOrigin().getLat() + "," + request.getOrigin().getLng(),
            request.getDestination().getLat() + "," + request.getDestination().getLng(),
            via, CommonUtils.formatTime(request.getRouteTime()), request.getTimeMode(), truckConfiguration,
            request.getMode() == GpsModeType.CHEAPEST, 0, true);
        RouteTimeline timeline = routeHereParser.parseTimeline(raw);
        if (timeline.portions().getLast().sectionIndex() != via.size()) {
            throw new TransportPlanningException("Les sections HERE ne correspondent pas aux points de passage du service.");
        }
        return timeline;
    }

    public Map<GpsModeType, RoutePtvParser.ParsedRoute> calculateRoutes(RouteRequest request, TruckConfiguration truckConfiguration, double driverHourlyRate) {
        return calculateRoutes(request,truckConfiguration,driverHourlyRate,null);
    }

    // onlyMode != null : un seul appel fournisseur pour ce mode
    public Map<GpsModeType, RoutePtvParser.ParsedRoute> calculateRoutes(RouteRequest request, TruckConfiguration truckConfiguration, double driverHourlyRate, GpsModeType onlyMode) {

        List<String> waypoints = new java.util.ArrayList<>();
        waypoints.add(request.getOrigin().getLat() + "," + request.getOrigin().getLng());
        if (request.getWaypoints() != null) {
            request.getWaypoints().stream().filter(point -> point != null).forEach(point -> waypoints.add(point.getLat() + "," + point.getLng()));
        }
        waypoints.add(request.getDestination().getLat() + "," + request.getDestination().getLng());

        String routeTime = request.getRouteTime() == null ? CommonUtils.formatTime(OffsetDateTime.now()) : CommonUtils.formatTime(request.getRouteTime());
        RouteTimeMode timeMode = request.getTimeMode() == null ? RouteTimeMode.DEPARTURE : request.getTimeMode();

        Map<GpsModeType, RoutePtvParser.ParsedRoute> routes = new LinkedHashMap<>();
        for (GpsModeType mode : onlyMode == null ? GpsModeType.values() : new GpsModeType[] { onlyMode }) {
            RoutePtvParser.ParsedRoute parsed = isHere() ? fetchHere(waypoints,routeTime,timeMode,truckConfiguration,mode) : fetchPtv(waypoints,truckConfiguration,routeTime,timeMode,mode,driverHourlyRate);
            if (parsed != null) {
                routes.put(mode,parsed);
            }
        }
        return routes;
    }

    private RoutePtvParser.ParsedRoute fetchHere(List<String> waypoints, String routeTime, RouteTimeMode timeMode, TruckConfiguration truckConfiguration, GpsModeType mode) {
        List<String> viaPoints = waypoints.subList(1,waypoints.size() - 1);
        String raw = hereApiClient.getRoutes(waypoints.getFirst(),waypoints.getLast(),viaPoints,routeTime,timeMode,truckConfiguration,GpsModeType.CHEAPEST.equals(mode),0);
        return routeHereParser.parseRoutes(raw).stream().findFirst()
            .map(here -> new RoutePtvParser.ParsedRoute(here.duration, here.baseDuration, here.distanceMeters, here.polyline, here.tollCost, here.rawJson)).orElse(null);
    }

    private RoutePtvParser.ParsedRoute fetchPtv(List<String> waypoints, TruckConfiguration truckConfiguration, String routeTime, RouteTimeMode timeMode, GpsModeType mode,
        double driverHourlyRate) {
        String raw = ptvApiClient.getRoutes(waypoints,truckConfiguration,routeTime,timeMode,mode,driverHourlyRate);
        return routePtvParser.parseRoutes(raw).stream().findFirst().orElse(null);
    }
}