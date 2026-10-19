package com.optiroute.backend.service.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteHereParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ParsedRoute> parseRoutes(String json) {
        try {
            List<ParsedRoute> parsedRoutes = new ArrayList<>();

            JsonNode root = objectMapper.readTree(json);
            JsonNode routes = root.path("routes");

            for (JsonNode route : routes) {
                JsonNode sections = route.path("sections");
                long duration = 0L;
                long baseDuration = 0L;
                long distance = 0L;
                double tollCost = 0.0;
                List<String> polylines = new ArrayList<>();

                for (JsonNode section : sections) {
                    duration += section.path("summary").path("duration").asLong();
                    baseDuration += section.path("summary").path("baseDuration").asLong();
                    distance += section.path("summary").path("length").asLong();
                    tollCost += extractTollCost(section);

                    String polyline = section.path("polyline").asText();
                    if (!polyline.isBlank()) {
                        polylines.add(polyline);
                    }
                }

                String polyline = serializePolylines(polylines);
                String rawJson = sections.toString();

                parsedRoutes.add(new ParsedRoute(duration, baseDuration, distance, polyline, tollCost, rawJson));
            }

            return parsedRoutes;

        } catch (Exception e) {
            throw new RuntimeException("HERE parsing failed", e);
        }
    }

    private String serializePolylines(List<String> polylines) throws Exception {
        if (polylines.isEmpty()) {
            return "";
        }

        if (polylines.size() == 1) {
            return polylines.getFirst();
        }

        return objectMapper.writeValueAsString(polylines);
    }

    private double extractTollCost(JsonNode section) {
        JsonNode tolls = section.path("tolls");
        if (!tolls.isArray()) {
            return 0.0;
        }

        double sum = 0.0;
        for (JsonNode tollSystem : tolls) {
            JsonNode fares = tollSystem.path("fares");
            double lowestFare = Double.POSITIVE_INFINITY;
            if (fares.isArray()) {
                for (JsonNode fare : fares) {
                    lowestFare = Math.min(lowestFare, fare.path("price").path("value").asDouble(Double.POSITIVE_INFINITY));
                }
            }
            if (Double.isFinite(lowestFare)) {
                sum += lowestFare;
            }
        }

        return sum;
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