package com.optiroute.backend.service.route;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

@Service
public class RoutePtvParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ParsedRoute> parseRoutes(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ParsedRoute> routes = new ArrayList<>();
            routes.add(parseRoute(root));

            JsonNode alternatives = root.path("alternativeRoutes");
            if (alternatives.isArray()) {
                for (JsonNode alternative : alternatives) {
                    routes.add(parseRoute(alternative));
                }
            }
            return routes;
        } catch (Exception exception) {
            throw new RuntimeException("PTV response parsing failed", exception);
        }
    }

    private ParsedRoute parseRoute(JsonNode route) {
        long duration = route.path("travelTime").asLong();
        long trafficDelay = route.path("trafficDelay").asLong(0L);
        long baseDuration = Math.max(0L,duration - trafficDelay);
        long distance = route.path("distance").asLong();
        double tollCost = extractTollCost(route.path("toll").path("costs"));
        String polyline = route.path("polyline").asText("");
        return new ParsedRoute(duration, baseDuration, distance, polyline, tollCost, route.toString());
    }

    private double extractTollCost(JsonNode costs) {
        JsonNode convertedPrice = costs.path("convertedPrice");
        if (convertedPrice.has("price")) {
            return convertedPrice.path("price").asDouble(0.0);
        }

        JsonNode prices = costs.path("prices");
        return prices.isArray() && !prices.isEmpty() ? prices.get(0).path("price").asDouble(0.0) : 0.0;
    }

    public static class ParsedRoute {

        public long duration;
        public long baseDuration;
        public long distanceMeters;
        public String polyline;
        public double tollCost;
        public String rawJson;

        public ParsedRoute(long duration, long baseDuration, long distanceMeters, String polyline, double tollCost, String rawJson) {
            this.duration = duration;
            this.baseDuration = baseDuration;
            this.distanceMeters = distanceMeters;
            this.polyline = polyline;
            this.tollCost = tollCost;
            this.rawJson = rawJson;
        }
    }
}