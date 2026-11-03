package com.optiroute.backend.service.transport;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.request.transport.ServiceTiming;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.exception.TransportPlanningException;
import com.optiroute.backend.mapper.RouteRequestFactory;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.model.RouteTimeline;
import com.optiroute.backend.service.driver.DriverService;
import com.optiroute.backend.service.route.RouteOptimizationService;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.type.driver.DriverType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class TransportRoutePlanner {

    private static final ZoneId PLANNING_ZONE = ZoneId.of("Europe/Paris");

    private final DriverService driverService;
    private final RouteRequestFactory routeRequestFactory;
    private final RouteOptimizationService routeOptimizationService;

    List<TransportRouteSegment> plan(TransportRequest transport, RouteDto selectedRoute) {
        ServiceTiming timing = transport.serviceTiming();
        OffsetDateTime deadline = deliveryDeadline(transport);
        OffsetDateTime departure;
        if (timing != null && timing.departureStart() != null) {
            departure = timing.departureStart().plusMinutes(minutes(timing.loadingDurationMinutes()));
        } else if (transport.routeTime() != null && transport.timeMode() != RouteTimeMode.ARRIVAL) {
            departure = transport.routeTime();
        } else if (deadline != null) {
            departure = latestDeparture(deadline, selectedRoute.getDuration());
        } else {
            return List.of(new TransportRouteSegment(transport, selectedRoute, false));
        }
        departure = normalizeDeparture(departure, deadline, selectedRoute.getDuration());
        boolean departureIsFixed = (timing != null && timing.departureStart() != null)
            || (transport.routeTime() != null && transport.timeMode() != RouteTimeMode.ARRIVAL);
        GpsModeType mode = selectedRoute.getType() == null ? GpsModeType.FASTEST : selectedRoute.getType();
        Driver driver = transport.driverId() == null ? null : driverService.getEntityById(transport.driverId());
        boolean regional = driver != null && driver.getDriverType() == DriverType.REGIONAL;
        TransportRequest current = timedRequest(transport, departure, RouteTimeMode.DEPARTURE);
        OffsetDateTime availableFrom = departureIsFixed ? departure : null;
        List<TransportRouteSegment> segments = new ArrayList<>();

        if (regional && deadline != null && date(deadline).isAfter(date(departure))) {
            current = returnToParking(current, driver, deadline, mode, segments);
            availableFrom = parkingAvailability(segments.getLast());
        }

        while (true) {
            RouteDto route = calculate(current, mode);
            OffsetDateTime start = current.timeMode() == RouteTimeMode.ARRIVAL
                ? current.routeTime().minusSeconds(route.getDuration()) : current.routeTime();
            if (availableFrom != null && start.isBefore(availableFrom)) {
                throw conflict("le départ recalculé précède la disponibilité du conducteur après la coupure.");
            }
            OffsetDateTime cutoff = at(date(start), 20);
            OffsetDateTime arrival = start.plusSeconds(route.getDuration());
            if (!arrival.isAfter(cutoff) && deadline != null && arrival.isAfter(deadline)) {
                current = timedRequest(current, deadline, RouteTimeMode.ARRIVAL);
                route = calculate(current, mode);
                start = current.routeTime().minusSeconds(route.getDuration());
                if (availableFrom != null && start.isBefore(availableFrom)) {
                    throw conflict("le départ recalculé précède la disponibilité du conducteur après la coupure.");
                }
                cutoff = at(date(start), 20);
                arrival = start.plusSeconds(route.getDuration());
            }
            boolean overnight = arrival.isAfter(cutoff)
                || (deadline != null && date(deadline).isAfter(date(start)));
            RouteTimeline timeline = null;
            if (overnight && driver != null && driver.getDriverType() == DriverType.SHORT_DISTANCE
                    && driver.getLocationLatitude() != null && driver.getLocationLongitude() != null) {
                timeline = routeOptimizationService.calculateTimeline(calculationRequest(current, mode));
                List<TransportRouteSegment> parkingSegments = planOptionalParking(
                    timedRequest(current, start, RouteTimeMode.DEPARTURE), driver, deadline, mode, timeline);
                if (!parkingSegments.isEmpty()) {
                    segments.addAll(parkingSegments);
                    return segments;
                }
            }
            if (!arrival.isAfter(cutoff)) {
                if (deadline != null && arrival.isAfter(deadline)) {
                    throw conflict("la livraison ne peut pas être réalisée dans la fenêtre prévue.");
                }
                segments.add(new TransportRouteSegment(current, route, true));
                return segments;
            }
            if (regional && !segments.isEmpty()) {
                throw conflict("la destination est inaccessible avant 20 h depuis le stationnement du régional.");
            }
            if (regional) {
                current = returnToParking(current, driver, deadline, mode, segments);
                availableFrom = parkingAvailability(segments.getLast());
                continue;
            }

            if (timeline == null) {
                timeline = routeOptimizationService.calculateTimeline(calculationRequest(current, mode));
            }
            Duration interval = Duration.between(start, cutoff);
            double elapsed = interval.getSeconds() + interval.getNano() / 1_000_000_000.0;
            if (elapsed <= 0 || elapsed >= timeline.durationSeconds()) {
                throw conflict("la chronologie HERE est incompatible avec la durée du trajet à découper.");
            }
            RouteTimeline.Cut cut = timeline.positionAt(elapsed);
            List<Position> waypoints = current.waypoints() == null ? List.of() : current.waypoints();
            if (cut.remainingWaypointIndex() > waypoints.size()) {
                throw conflict("les sections HERE ne correspondent pas aux points de passage.");
            }
            Position stop = cut.position();
            TransportRequest first = segmentRequest(current, current.originName(), current.originAddress(),
                current.originLat(), current.originLng(), "Arrêt de nuit à 20 h", null, stop.getLat(), stop.getLng(),
                waypoints.subList(0, cut.firstWaypointCount()), cutoff, RouteTimeMode.ARRIVAL);
            RouteDto firstRoute = calculate(first, mode);
            OffsetDateTime firstDeparture = cutoff.minusSeconds(firstRoute.getDuration());
            if (firstDeparture.isBefore(start) || !date(firstDeparture).equals(date(start))) {
                throw conflict("rejoindre le point d'arrêt à 20 h impose un départ avant l'heure disponible.");
            }
            segments.add(new TransportRouteSegment(first, firstRoute, true));
            TransportRequest remaining = segmentRequest(current, "Arrêt de nuit à 20 h", null,
                stop.getLat(), stop.getLng(), current.destinationName(), current.destinationAddress(),
                current.destinationLat(), current.destinationLng(),
                waypoints.subList(cut.remainingWaypointIndex(), waypoints.size()), cutoff, RouteTimeMode.DEPARTURE);
            current = resume(remaining, cutoff, date(cutoff).plusDays(1), deadline, mode);
            availableFrom = at(date(cutoff).plusDays(1), 0);
        }
    }

    private List<TransportRouteSegment> planOptionalParking(TransportRequest current, Driver driver,
            OffsetDateTime deadline, GpsModeType mode, RouteTimeline timeline) {
        double lat = driver.getLocationLatitude().doubleValue();
        double lng = driver.getLocationLongitude().doubleValue();
        RouteTimeline.Proximity proximity = timeline.proximityTo(new Position(lat, lng));
        if (proximity.distanceMeters() >= 50_000) {
            return List.of();
        }
        List<Position> waypoints = current.waypoints() == null ? List.of() : current.waypoints();
        int firstWaypointCount = Math.min(proximity.firstWaypointCount(), waypoints.size());
        int remainingWaypointIndex = Math.min(proximity.remainingWaypointIndex(), waypoints.size());
        TransportRequest parking = segmentRequest(current, current.originName(), current.originAddress(),
            current.originLat(), current.originLng(), driver.getLocationLabel(), driver.getLocationLabel(),
            lat, lng, waypoints.subList(0, firstWaypointCount), current.routeTime(), RouteTimeMode.DEPARTURE);
        RouteDto firstRoute = calculate(parking, mode);
        OffsetDateTime parkingArrival = parking.routeTime().plusSeconds(firstRoute.getDuration());
        LocalDate nextDay = date(current.routeTime()).plusDays(1);
        OffsetDateTime morning = at(nextDay, 5);
        OffsetDateTime evening = at(nextDay, 20);
        OffsetDateTime target = deadline == null || deadline.isAfter(evening) ? evening : deadline;
        if (target.isBefore(morning)) {
            return List.of();
        }
        TransportRequest remaining = segmentRequest(current, driver.getLocationLabel(), driver.getLocationLabel(),
            lat, lng, current.destinationName(), current.destinationAddress(), current.destinationLat(),
            current.destinationLng(), waypoints.subList(remainingWaypointIndex, waypoints.size()),
            deadline == null ? morning : target, deadline == null ? RouteTimeMode.DEPARTURE : RouteTimeMode.ARRIVAL);
        RouteDto secondRoute = calculate(remaining, mode);
        OffsetDateTime secondDeparture = deadline == null ? morning : target.minusSeconds(secondRoute.getDuration());
        OffsetDateTime secondArrival = secondDeparture.plusSeconds(secondRoute.getDuration());
        if (secondDeparture.isBefore(morning) || secondDeparture.isBefore(parkingArrival)
                || secondArrival.isAfter(evening) || (deadline != null && secondArrival.isAfter(deadline))) {
            return List.of();
        }
        return List.of(new TransportRouteSegment(parking, firstRoute, true),
            new TransportRouteSegment(remaining, secondRoute, true));
    }

    private TransportRequest returnToParking(TransportRequest current, Driver driver, OffsetDateTime deadline,
            GpsModeType mode, List<TransportRouteSegment> segments) {
        if (driver.getLocationLatitude() == null || driver.getLocationLongitude() == null) {
            throw conflict("le lieu de stationnement du régional est manquant.");
        }
        double lat = driver.getLocationLatitude().doubleValue();
        double lng = driver.getLocationLongitude().doubleValue();
        TransportRequest parking = segmentRequest(current, current.originName(), current.originAddress(),
            current.originLat(), current.originLng(), driver.getLocationLabel(), driver.getLocationLabel(),
            lat, lng, current.waypoints(), current.routeTime(), RouteTimeMode.DEPARTURE);
        RouteDto route = calculate(parking, mode);
        OffsetDateTime arrival = parking.routeTime().plusSeconds(route.getDuration());
        segments.add(new TransportRouteSegment(parking, route, true));
        TransportRequest remaining = segmentRequest(current, driver.getLocationLabel(), driver.getLocationLabel(),
            lat, lng, current.destinationName(), current.destinationAddress(), current.destinationLat(),
            current.destinationLng(), List.of(), arrival, RouteTimeMode.DEPARTURE);
        return resume(remaining, arrival, date(parking.routeTime()).plusDays(1), deadline, mode);
    }

    private TransportRequest resume(TransportRequest remaining, OffsetDateTime lastArrival,
            LocalDate nextDay, OffsetDateTime deadline, GpsModeType mode) {
        OffsetDateTime morning = at(nextDay, 5);
        if (deadline == null) {
            if (lastArrival.isAfter(morning)) {
                morning = normalizeDeparture(lastArrival, null, 0);
            }
            return timedRequest(remaining, morning, RouteTimeMode.DEPARTURE);
        }
        OffsetDateTime target = deadline.isAfter(at(date(deadline), 20)) ? at(date(deadline), 20) : deadline;
        if (date(target).isBefore(nextDay)) {
            throw conflict("la livraison est prévue avant la reprise du lendemain.");
        }
        RouteDto estimate = calculate(timedRequest(remaining, target, RouteTimeMode.ARRIVAL), mode);
        long available = availableDriving(morning, target);
        OffsetDateTime departure;
        if (estimate.getDuration() <= available) {
            departure = latestDeparture(target, estimate.getDuration());
        } else {
            OffsetDateTime earlyReference = target.isBefore(morning) ? target : morning;
            departure = earlyReference.minusSeconds(estimate.getDuration() - available);
            if (departure.isBefore(at(nextDay, 0))) {
                throw conflict("la livraison impose une reprise avant le lendemain de la coupure à 20 h.");
            }
        }
        if (!departure.isAfter(lastArrival) || !departure.isBefore(target)) {
            throw conflict("la reprise et la livraison sont incompatibles avec la coupure nocturne.");
        }
        return date(departure).equals(date(target))
            ? timedRequest(remaining, target, RouteTimeMode.ARRIVAL)
            : timedRequest(remaining, departure, RouteTimeMode.DEPARTURE);
    }

    private OffsetDateTime normalizeDeparture(OffsetDateTime departure, OffsetDateTime deadline, long duration) {
        LocalTime time = departure.atZoneSameInstant(PLANNING_ZONE).toLocalTime();
        if (!time.isBefore(LocalTime.of(20, 0))) {
            return at(date(departure).plusDays(1), 5);
        }
        OffsetDateTime morning = at(date(departure), 5);
        if (departure.isBefore(morning)) {
            if (deadline != null && duration > availableDriving(morning, deadline)) {
                return departure;
            }
            return morning;
        }
        return departure;
    }

    private OffsetDateTime latestDeparture(OffsetDateTime arrival, long duration) {
        OffsetDateTime end = arrival.isAfter(at(date(arrival), 20)) ? at(date(arrival), 20) : arrival;
        long remaining = duration;
        while (remaining > 0) {
            OffsetDateTime morning = at(date(end), 5);
            long available = Math.max(0, Duration.between(morning, end).getSeconds());
            if (remaining <= available) {
                return end.minusSeconds(remaining);
            }
            remaining -= available;
            end = at(date(end).minusDays(1), 20);
        }
        return end;
    }

    private long availableDriving(OffsetDateTime start, OffsetDateTime deadline) {
        long seconds = 0;
        for (LocalDate day = date(start); !day.isAfter(date(deadline)); day = day.plusDays(1)) {
            OffsetDateTime from = day.equals(date(start)) ? start : at(day, 5);
            OffsetDateTime end = at(day, 20).isAfter(deadline) ? deadline : at(day, 20);
            seconds += Math.max(0, Duration.between(from, end).getSeconds());
        }
        return seconds;
    }

    private OffsetDateTime deliveryDeadline(TransportRequest transport) {
        ServiceTiming timing = transport.serviceTiming();
        OffsetDateTime arrival = timing == null ? null
            : timing.arrivalEnd() != null ? timing.arrivalEnd() : timing.arrivalStart();
        if (arrival != null) {
            return arrival.minusMinutes(minutes(timing.unloadingDurationMinutes()));
        }
        return transport.timeMode() == RouteTimeMode.ARRIVAL ? transport.routeTime() : null;
    }

    private RouteDto calculate(TransportRequest transport, GpsModeType mode) {
        RouteDto route = routeOptimizationService.calculateRoute(calculationRequest(transport, mode), mode)
            .getRoutes().stream().findFirst()
            .orElseThrow(() -> conflict("aucun itinéraire disponible pour le segment du service."));
        if (route.getDuration() < 0) {
            throw conflict("la durée du trajet est invalide.");
        }
        return route;
    }

    private RouteRequest calculationRequest(TransportRequest transport, GpsModeType mode) {
        RouteRequest request = routeRequestFactory.fromTransport(transport);
        request.setRouteTime(transport.routeTime());
        request.setTimeMode(transport.timeMode());
        request.setDriverId(transport.driverId());
        request.setMode(mode);
        return request;
    }

    private TransportRequest timedRequest(TransportRequest source, OffsetDateTime time, RouteTimeMode mode) {
        return segmentRequest(source, source.originName(), source.originAddress(), source.originLat(), source.originLng(),
            source.destinationName(), source.destinationAddress(), source.destinationLat(), source.destinationLng(),
            source.waypoints(), time, mode);
    }

    private TransportRequest segmentRequest(TransportRequest source, String originName, String originAddress,
            double originLat, double originLng, String destinationName, String destinationAddress, double destinationLat,
            double destinationLng, List<Position> waypoints, OffsetDateTime routeTime, RouteTimeMode timeMode) {
        return new TransportRequest(source.name(), source.driverId(), source.tractorId(), source.semiTrailerId(),
            source.customerId(), source.emptyTrip(), source.externalId(), source.externalSource(), routeTime, timeMode,
            originName, originAddress, originLat, originLng, destinationName, destinationAddress, destinationLat,
            destinationLng, source.revenue(), source.serviceTiming(), waypoints == null ? List.of() : new ArrayList<>(waypoints));
    }

    private static LocalDate date(OffsetDateTime time) {
        return time.atZoneSameInstant(PLANNING_ZONE).toLocalDate();
    }

    private static OffsetDateTime at(LocalDate date, int hour) {
        return date.atTime(hour, 0).atZone(PLANNING_ZONE).toOffsetDateTime();
    }

    private static int minutes(Integer value) {
        return value == null ? 0 : value;
    }

    private static OffsetDateTime arrival(TransportRouteSegment segment) {
        return segment.request().timeMode() == RouteTimeMode.ARRIVAL ? segment.request().routeTime()
            : segment.request().routeTime().plusSeconds(segment.route().getDuration());
    }

    private static OffsetDateTime parkingAvailability(TransportRouteSegment segment) {
        OffsetDateTime midnight = at(date(segment.request().routeTime()).plusDays(1), 0);
        OffsetDateTime arrival = arrival(segment);
        return arrival.isAfter(midnight) ? arrival : midnight;
    }

    private static TransportPlanningException conflict(String reason) {
        return new TransportPlanningException("Planning incompatible : " + reason);
    }
}
