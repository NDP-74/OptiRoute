package com.optiroute.backend.dto.request.transport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.optiroute.backend.model.Position;
import com.optiroute.backend.type.RouteTimeMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TransportRequest(String name,

        Long driverId, Long tractorId, Long semiTrailerId, Long customerId,

        boolean emptyTrip,

        @Size(max = 255) String externalId,
        @Pattern(regexp = "(?i)MANUAL|DASHDOC") String externalSource,

        OffsetDateTime routeTime, RouteTimeMode timeMode,

        String originName, String originAddress, double originLat, double originLng,

        String destinationName, String destinationAddress, double destinationLat, double destinationLng,

        BigDecimal revenue,

        @Valid ServiceTiming serviceTiming,
        List<@NotNull @Valid Position> waypoints) {

}