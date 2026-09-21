package com.optiroute.backend.client;

import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.optiroute.backend.config.PtvApiProperties;
import com.optiroute.backend.model.TruckConfiguration;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;

@Service
public class PtvApiClient {

    private final RestClient restClient;
    private final PtvApiProperties properties;

    public PtvApiClient(RestClient.Builder builder, PtvApiProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public String getRoutes(List<String> waypoints, TruckConfiguration truckConfiguration, String routeTime, RouteTimeMode timeMode, GpsModeType mode, double driverHourlyRate) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(properties.getBaseUrl() + "/routes").queryParam("profile",properties.getProfile())
            .queryParam("vehicle[totalPermittedWeight]",truckConfiguration.getWeightKg()).queryParam("vehicle[height]",truckConfiguration.getHeightCm())
            .queryParam("vehicle[width]",truckConfiguration.getWidthCm()).queryParam("vehicle[length]",truckConfiguration.getLengthCm())
            .queryParam("vehicle[numberOfAxles]",truckConfiguration.getAxleCount()).queryParam("vehicle[averageFuelConsumption]",truckConfiguration.getAverageConsumption())
            .queryParam("results","POLYLINE,TOLL_COSTS").queryParam("options[routingMode]",GpsModeType.CHEAPEST.equals(mode) ? "MONETARY" : "FAST");
        
        if (GpsModeType.CHEAPEST.equals(mode)){
            uriBuilder.queryParam("monetaryCostOptions[workingCostPerHour]",driverHourlyRate);
        }

        if (routeTime != null && !routeTime.isBlank()) {
            String timeParameter = RouteTimeMode.ARRIVAL.equals(timeMode) ? "options[arrivalTime]" : "options[startTime]";
            uriBuilder.queryParam(timeParameter,routeTime);
        }

        for (String waypoint : waypoints) {
            uriBuilder.queryParam("waypoints",waypoint);
        }

        URI uri = uriBuilder.build().encode().toUri();
        return restClient.get().uri(uri).header("apiKey",properties.getApiKey()).retrieve().body(String.class);
    }
}