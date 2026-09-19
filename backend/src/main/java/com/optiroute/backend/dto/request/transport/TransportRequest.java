package com.optiroute.backend.dto.request.transport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransportRequest(String name,

        Long driverId, Long tractorId, Long semiTrailerId, Long customerId,

        boolean emptyTrip,

        OffsetDateTime plannedStart, OffsetDateTime plannedEnd,

        String originName, String originAddress, double originLat, double originLng,

        String destinationName, String destinationAddress, double destinationLat, double destinationLng,

        BigDecimal revenue) {
}