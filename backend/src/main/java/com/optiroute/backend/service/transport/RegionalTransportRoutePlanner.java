package com.optiroute.backend.service.transport;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.request.transport.ServiceTiming;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.mapper.RouteRequestFactory;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.service.driver.DriverService;
import com.optiroute.backend.service.route.RouteOptimizationService;
import com.optiroute.backend.type.RouteTimeMode;
import com.optiroute.backend.type.driver.DriverType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class RegionalTransportRoutePlanner {

    private static final ZoneId PLANNING_ZONE = ZoneId.of("Europe/Paris");

    private final DriverService driverService;
    private final RouteRequestFactory routeRequestFactory;
    private final RouteOptimizationService routeOptimizationService;

    List<TransportRouteSegment> plan(TransportRequest transport, RouteDto selectedRoute) {
        ServiceTiming timing = transport.serviceTiming();
        if (timing == null) {
            return List.of(new TransportRouteSegment(transport, selectedRoute, false));
        }
        OffsetDateTime arrivalEnd = timing.arrivalEnd();
        OffsetDateTime startAnchor = firstNonNull(timing.departureStart(), timing.arrivalStart(), transport.routeTime());

        if (transport.driverId() == null || arrivalEnd == null || startAnchor == null
            || !arrivalEnd.atZoneSameInstant(PLANNING_ZONE).toLocalDate()
                .equals(startAnchor.atZoneSameInstant(PLANNING_ZONE).toLocalDate().plusDays(1))) {
            return List.of(new TransportRouteSegment(transport, selectedRoute, false));
        }

        Driver driver = driverService.getEntityById(transport.driverId());
        if (driver.getDriverType() != DriverType.REGIONAL) {
            return List.of(new TransportRouteSegment(transport, selectedRoute, false));
        }

        Position parking = new Position(driver.getLocationLatitude().doubleValue(), driver.getLocationLongitude().doubleValue());
        parking.setName(driver.getLocationLabel());
        parking.setAddress(driver.getLocationLabel());

        OffsetDateTime firstRouteTime;
        RouteTimeMode firstTimeMode;
        if (timing.departureStart() != null) {
            firstRouteTime = timing.departureStart();
            firstTimeMode = RouteTimeMode.DEPARTURE;
        } else if (timing.arrivalStart() != null) {
            firstRouteTime = timing.arrivalStart();
            firstTimeMode = RouteTimeMode.ARRIVAL;
        } else {
            firstRouteTime = transport.routeTime();
            firstTimeMode = transport.timeMode() == null ? RouteTimeMode.DEPARTURE : transport.timeMode();
        }
        TransportRequest firstRequest = segmentRequest(transport, transport.originName(), transport.originAddress(),
            transport.originLat(), transport.originLng(), driver.getLocationLabel(), driver.getLocationLabel(),
            parking.getLat(), parking.getLng(), transport.waypoints(), firstRouteTime, firstTimeMode);

        OffsetDateTime unloadingEnd = arrivalEnd.minusMinutes(timing.unloadingDurationMinutes() == null
            ? 0 : timing.unloadingDurationMinutes());
        TransportRequest secondRequest = segmentRequest(transport, driver.getLocationLabel(), driver.getLocationLabel(),
            parking.getLat(), parking.getLng(), transport.destinationName(), transport.destinationAddress(),
            transport.destinationLat(), transport.destinationLng(), List.of(), unloadingEnd, RouteTimeMode.ARRIVAL);

        return List.of(
            new TransportRouteSegment(firstRequest, calculateFastest(firstRequest), true),
            new TransportRouteSegment(secondRequest, calculateFastest(secondRequest), true));
    }

    private RouteDto calculateFastest(TransportRequest transport) {
        RouteRequest request = routeRequestFactory.fromTransport(transport);
        request.setRouteTime(transport.routeTime());
        request.setTimeMode(transport.timeMode());
        request.setDriverId(transport.driverId());
        RoutesResponse response = routeOptimizationService.calculateRoute(request);
        return response.getRoutes().stream()
            .min((first, second) -> Long.compare(first.getDuration(), second.getDuration()))
            .orElseThrow(() -> new IllegalArgumentException("No route available for regional service segment"));
    }

    private TransportRequest segmentRequest(TransportRequest source, String originName, String originAddress,
        double originLat, double originLng, String destinationName, String destinationAddress, double destinationLat,
        double destinationLng, List<Position> waypoints, OffsetDateTime routeTime, RouteTimeMode timeMode) {
        return new TransportRequest(source.name(), source.driverId(), source.tractorId(), source.semiTrailerId(),
            source.customerId(), source.emptyTrip(), source.externalId(), source.externalSource(), routeTime, timeMode,
            originName, originAddress, originLat, originLng, destinationName, destinationAddress, destinationLat,
            destinationLng, source.revenue(), source.serviceTiming(), new ArrayList<>(waypoints));
    }

    private OffsetDateTime firstNonNull(OffsetDateTime first, OffsetDateTime second, OffsetDateTime third) {
        if (first != null) {
            return first;
        }
        return second != null ? second : third;
    }
}
