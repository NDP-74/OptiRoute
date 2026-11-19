package com.optiroute.backend.service.dashdoc;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.optiroute.backend.dto.request.transport.ServiceTiming;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.type.RouteTimeMode;

@Component
public class DashdocTransportMapper {

    private static final ZoneId PLANNING_ZONE = ZoneId.of("Europe/Paris");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{Alnum}]");

    public String externalId(JsonNode transport) {
        String uid = text(transport, "uid");
        String remoteId = text(transport, "remote_id");
        String externalId = uid != null ? uid : remoteId;
        if (externalId == null) {
            throw new DashdocTransportMappingException("Identifiant Dashdoc absent");
        }
        return externalId;
    }

    public TransportRequest map(
            JsonNode transport,
            LocalDate startDate,
            LocalDate endDate,
            List<Driver> drivers,
            List<Tractor> tractors,
            List<SemiTrailer> semiTrailers,
            Integer loadingDurationMinutes,
            Integer unloadingDurationMinutes) {
        externalId(transport);

        JsonNode deliveries = transport.path("deliveries");
        if (!deliveries.isArray() || deliveries.size() != 1) {
            throw new DashdocTransportMappingException("Le transport ne contient pas exactement une livraison");
        }

        List<ScheduledSegment> scheduledSegments = readScheduledSegments(transport.path("segments"));
        if (!hasScheduledDateInRange(scheduledSegments, startDate, endDate)) {
            throw new DashdocTransportOutsideRangeException();
        }
        ScheduledSegment firstSegment = scheduledSegments.getFirst();
        ScheduledSegment lastSegment = scheduledSegments.getLast();
        OffsetDateTime scheduledDeparture = readScheduledDeparture(firstSegment);

        Address start = readAddress(firstSegment.source().path("origin").path("address"), "chargement");
        Address end = readAddress(lastSegment.source().path("destination").path("address"), "livraison");
        Assignment assignment = readAssignment(transport.path("segments"), drivers, tractors, semiTrailers);

        String reference = text(transport, "carrier_reference");
        if (reference == null) {
            reference = text(transport, "sequential_id");
        }
        String name = reference == null ? "Transport Dashdoc " + externalId(transport) : "Transport Dashdoc " + reference;

        JsonNode revenueNode = transport.path("pricing_total_price");
        BigDecimal revenue = revenueNode.isNumber()
            ? revenueNode.decimalValue()
            : parseDecimal(revenueNode.asText(null));

        ServiceTiming timing = new ServiceTiming(
            loadingDurationMinutes,
            unloadingDurationMinutes,
            firstSegment.startRange().start(),
            firstSegment.startRange().end(),
            lastSegment.endRange().start(),
            lastSegment.endRange().end());

        return new TransportRequest(
            name,
            assignment.driver().getId(),
            assignment.tractor().getId(),
            assignment.semiTrailer() == null ? null : assignment.semiTrailer().getId(),
            null,
            false,
            externalId(transport),
            "DASHDOC",
            scheduledDeparture,
            RouteTimeMode.DEPARTURE,
            start.name(),
            start.address(),
            start.latitude(),
            start.longitude(),
            end.name(),
            end.address(),
            end.latitude(),
            end.longitude(),
            revenue,
            timing,
            List.of());
    }

    private static List<ScheduledSegment> readScheduledSegments(JsonNode segments) {
        if (!segments.isArray() || segments.isEmpty()) {
            throw new DashdocTransportMappingException("Plages horaires planifiées Dashdoc absentes");
        }

        List<ScheduledSegment> result = new ArrayList<>();
        int index = 0;
        for (JsonNode segment : segments) {
            TimeWindow startRange = readScheduledRange(segment.path("scheduled_start_range"), "début");
            TimeWindow endRange = readScheduledRange(segment.path("scheduled_end_range"), "fin");
            int sortOrder = segment.path("sort_order").canConvertToInt()
                ? segment.path("sort_order").asInt()
                : index;
            result.add(new ScheduledSegment(segment, startRange, endRange, sortOrder));
            index++;
        }
        result.sort(Comparator.comparingInt(ScheduledSegment::sortOrder));
        return result;
    }

    private static TimeWindow readScheduledRange(JsonNode range, String label) {
        try {
            TimeWindow result = new TimeWindow(
                OffsetDateTime.parse(range.path("start").asText()),
                OffsetDateTime.parse(range.path("end").asText()));
            if (result.start().isAfter(result.end())) {
                throw new DashdocTransportMappingException("Plage horaire planifiée Dashdoc invalide (" + label + ")");
            }
            return result;
        } catch (DateTimeParseException exception) {
            throw new DashdocTransportMappingException("Plage horaire planifiée Dashdoc invalide (" + label + ")");
        }
    }

    private static OffsetDateTime readScheduledDeparture(ScheduledSegment segment) {
        String scheduledStart = text(segment.source(), "scheduled_start");
        if (scheduledStart == null) {
            return segment.startRange().start();
        }
        try {
            OffsetDateTime departure = OffsetDateTime.parse(scheduledStart);
            if (departure.isBefore(segment.startRange().start()) || departure.isAfter(segment.startRange().end())) {
                throw new DashdocTransportMappingException("scheduled_start est hors de sa plage planifiée");
            }
            return departure;
        } catch (DateTimeParseException exception) {
            throw new DashdocTransportMappingException("Heure scheduled_start invalide");
        }
    }

    private static Address readAddress(JsonNode address, String label) {
        JsonNode latitude = address.path("latitude");
        JsonNode longitude = address.path("longitude");
        if (!latitude.isNumber() || !longitude.isNumber()) {
            throw new DashdocTransportMappingException("Coordonnées absentes pour le " + label);
        }

        double lat = latitude.asDouble();
        double lng = longitude.asDouble();
        if (!Double.isFinite(lat) || lat < -90 || lat > 90 || !Double.isFinite(lng) || lng < -180 || lng > 180) {
            throw new DashdocTransportMappingException("Coordonnées invalides pour le " + label);
        }

        String name = text(address, "name");
        String addressLine = text(address, "address");
        String city = text(address, "city");
        String postcode = text(address, "postcode");
        StringBuilder fullAddress = new StringBuilder();
        appendAddressPart(fullAddress, addressLine);
        appendAddressPart(fullAddress, postcode);
        appendAddressPart(fullAddress, city);

        return new Address(
            name == null ? fullAddress.toString() : name,
            fullAddress.isEmpty() ? null : fullAddress.toString(),
            lat,
            lng);
    }

    private static Assignment readAssignment(
            JsonNode segments,
            List<Driver> drivers,
            List<Tractor> tractors,
            List<SemiTrailer> semiTrailers) {
        if (!segments.isArray() || segments.isEmpty()) {
            throw new DashdocTransportMappingException("Conducteur ou véhicule Dashdoc absent");
        }

        Assignment assignment = null;
        for (JsonNode segment : segments) {
            Driver driver = findDriver(text(segment.path("trucker"), "display_name"), drivers);
            Tractor tractor = findTractor(text(segment.path("vehicle"), "license_plate"), tractors);
            SemiTrailer semiTrailer = findSemiTrailer(segment.path("trailers"), semiTrailers);
            Assignment current = new Assignment(driver, tractor, semiTrailer);
            if (assignment != null && !sameAssignment(assignment, current)) {
                throw new DashdocTransportMappingException("Le transport a plusieurs affectations différentes");
            }
            assignment = current;
        }
        return Objects.requireNonNull(assignment);
    }

    private static boolean hasScheduledDateInRange(
            List<ScheduledSegment> segments,
            LocalDate startDate,
            LocalDate endDate) {
        return segments.stream().anyMatch(segment ->
            segment.startRange().intersects(startDate, endDate)
                || segment.endRange().intersects(startDate, endDate));
    }

    private static Driver findDriver(String displayName, List<Driver> drivers) {
        String normalizedName = normalizeName(displayName);
        List<Driver> matches = drivers.stream()
            .filter(driver -> normalizeName(driver.getFirstName() + " " + driver.getLastName()).equals(normalizedName))
            .toList();
        if (normalizedName.isEmpty() || matches.size() != 1) {
            throw new DashdocTransportMappingException("Correspondance conducteur introuvable ou ambiguë");
        }
        return matches.getFirst();
    }

    private static Tractor findTractor(String registration, List<Tractor> tractors) {
        String normalizedRegistration = normalizeRegistration(registration);
        List<Tractor> matches = tractors.stream()
            .filter(tractor -> normalizeRegistration(tractor.getRegistration()).equals(normalizedRegistration))
            .toList();
        if (normalizedRegistration.isEmpty() || matches.size() != 1) {
            throw new DashdocTransportMappingException("Correspondance tracteur introuvable ou ambiguë");
        }
        return matches.getFirst();
    }

    private static SemiTrailer findSemiTrailer(JsonNode trailerNodes, List<SemiTrailer> semiTrailers) {
        if (trailerNodes.isMissingNode() || trailerNodes.isNull()
                || (trailerNodes.isArray() && trailerNodes.isEmpty())) {
            return null;
        }
        if (!trailerNodes.isArray() || trailerNodes.size() != 1) {
            throw new DashdocTransportMappingException("Le transport contient plusieurs remorques");
        }

        String registration = text(trailerNodes.get(0), "license_plate");
        String normalizedRegistration = normalizeRegistration(registration);
        List<SemiTrailer> matches = semiTrailers.stream()
            .filter(trailer -> normalizeRegistration(trailer.getRegistration()).equals(normalizedRegistration))
            .toList();
        if (normalizedRegistration.isEmpty() || matches.size() != 1) {
            throw new DashdocTransportMappingException("Correspondance semi-remorque introuvable ou ambiguë");
        }
        return matches.getFirst();
    }

    private static boolean sameAssignment(Assignment first, Assignment second) {
        return first.driver().getId().equals(second.driver().getId())
            && first.tractor().getId().equals(second.tractor().getId())
            && Objects.equals(
                first.semiTrailer() == null ? null : first.semiTrailer().getId(),
                second.semiTrailer() == null ? null : second.semiTrailer().getId());
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("")
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeRegistration(String value) {
        return value == null ? "" : NON_ALPHANUMERIC.matcher(value).replaceAll("").toUpperCase(java.util.Locale.ROOT);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isValueNode()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new DashdocTransportMappingException("Chiffre d’affaires Dashdoc invalide");
        }
    }

    private static void appendAddressPart(StringBuilder target, String value) {
        if (value == null) {
            return;
        }
        if (!target.isEmpty()) {
            target.append(", ");
        }
        target.append(value);
    }

    private record Address(String name, String address, double latitude, double longitude) {
    }

    private record Assignment(Driver driver, Tractor tractor, SemiTrailer semiTrailer) {
    }

    private record ScheduledSegment(JsonNode source, TimeWindow startRange, TimeWindow endRange, int sortOrder) {
    }

    private record TimeWindow(OffsetDateTime start, OffsetDateTime end) {
        boolean intersects(LocalDate startDate, LocalDate endDate) {
            LocalDate localStart = start.atZoneSameInstant(PLANNING_ZONE).toLocalDate();
            LocalDate localEnd = end.atZoneSameInstant(PLANNING_ZONE).toLocalDate();
            return !localStart.isAfter(endDate) && !localEnd.isBefore(startDate);
        }
    }
}
