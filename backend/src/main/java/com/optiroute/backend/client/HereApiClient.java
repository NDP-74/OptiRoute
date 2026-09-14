package com.optiroute.backend.client;

import java.net.URI;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.optiroute.backend.config.HereApiProperties;
import com.optiroute.backend.model.TruckConfiguration;

@Service
public class HereApiClient {

    private final RestClient restClient;
    private final HereApiProperties properties;

    public HereApiClient(RestClient.Builder builder, HereApiProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public String getRoutes(String origin, String destination, List<String> viaPoints, String departureTime, TruckConfiguration truckConfiguration, boolean avoidTolls,
        int alternatives) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("https://router.hereapi.com/v8/routes").queryParam("origin",origin)
            .queryParam("destination",destination).queryParam("alternatives",alternatives).queryParam("return","polyline,summary,tolls").queryParam("transportMode","truck")
            .queryParam("routingMode","fast")

            .queryParam("vehicle[currentWeight]",truckConfiguration.getWeightKg()).queryParam("vehicle[height]",truckConfiguration.getHeightCm())
            .queryParam("vehicle[width]",truckConfiguration.getWidthCm()).queryParam("vehicle[length]",truckConfiguration.getLengthCm())
            .queryParam("vehicle[axleCount]",truckConfiguration.getAxleCount()).queryParam("vehicle[speedCap]",truckConfiguration.getMaxSpeed())

            .queryParam("departureTime",departureTime).queryParam("apikey",properties.getApiKey());

        if (viaPoints != null) {
            for (String viaPoint : viaPoints) {
                if (viaPoint != null && !viaPoint.isBlank()) {
                    uriBuilder.queryParam("via",viaPoint);
                }
            }
        }

        if (avoidTolls) {
            uriBuilder.queryParam("avoid[features]","tollRoad");
        }

        URI uri = uriBuilder.build().encode().toUri();
        return restClient.get().uri(uri).retrieve().body(String.class);
    }
}