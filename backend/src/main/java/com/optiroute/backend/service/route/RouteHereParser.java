package com.optiroute.backend.service.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.model.RouteTimeline;
import com.optiroute.backend.utils.FlexiblePolylineDecoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteHereParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteTimeline parseTimeline(String json) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid HERE timeline response", exception);
        }
        JsonNode sections = root.path("routes").path(0).path("sections");
        if (!sections.isArray() || sections.isEmpty()) {
            throw new IllegalArgumentException("HERE did not return a route for the 20:00 cut");
        }
        List<RouteTimeline.Portion> portions = new ArrayList<>();
        long totalDuration = 0;
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            JsonNode section = sections.get(sectionIndex);
            List<Position> geometry = FlexiblePolylineDecoder.decode(section.path("polyline").asText());
            JsonNode spans = section.path("spans");
            long duration = section.path("summary").path("duration").asLong(-1);
            if (!spans.isArray() || spans.isEmpty() || duration < 0) {
                throw new IllegalArgumentException("HERE timed spans are missing");
            }
            double spanDuration = 0;
            for (JsonNode span : spans) {
                JsonNode value = span.path("duration");
                if (!value.isNumber() || !Double.isFinite(value.asDouble()) || value.asDouble() < 0) {
                    throw new IllegalArgumentException("Invalid HERE span duration");
                }
                spanDuration += value.asDouble();
            }
            if (spanDuration == 0 && duration > 0) {
                throw new IllegalArgumentException("HERE spans do not cover the section duration");
            }
            for (int index = 0; index < spans.size(); index++) {
                JsonNode span = spans.get(index);
                int start = span.path("offset").asInt(-1);
                int end = index + 1 < spans.size() ? spans.get(index + 1).path("offset").asInt(-1) : geometry.size() - 1;
                if (start < 0 || (index == 0 && start != 0) || end < start || end >= geometry.size()
                    || (start == end && span.path("duration").asDouble() > 0)) {
                    throw new IllegalArgumentException("Invalid HERE span geometry offsets");
                }
                // Span durations can be rounded independently of the section summary.
                double seconds = spanDuration == 0 ? 0 : span.path("duration").asDouble() * duration / spanDuration;
                portions.add(new RouteTimeline.Portion(List.copyOf(geometry.subList(start, end + 1)), seconds, sectionIndex));
            }
            totalDuration = Math.addExact(totalDuration, duration);
        }
        return new RouteTimeline(List.copyOf(portions), totalDuration);
    }

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