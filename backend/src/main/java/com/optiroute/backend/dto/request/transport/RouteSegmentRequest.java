package com.optiroute.backend.dto.request.transport;

import java.time.OffsetDateTime;
import java.util.List;

import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.type.RouteTimeMode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

// Modification d'un seul segment (route) d'un service, sans impact sur les autres segments
public record RouteSegmentRequest(
        @NotNull Long driverId, @NotNull Long tractorId, Long semiTrailerId,

        boolean emptyTrip,

        @NotNull OffsetDateTime routeTime, RouteTimeMode timeMode,

        String originName, String originAddress, double originLat, double originLng,

        String destinationName, String destinationAddress, double destinationLat, double destinationLng,

        List<@NotNull @Valid Position> waypoints,

        @NotNull RouteDto selectedRoute) {
}
