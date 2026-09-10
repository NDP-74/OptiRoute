package com.optiroute.backend.dto.response.transport;

import java.time.OffsetDateTime;

public record TransportPlanningResponse(

        Long id, String name,

        Long driverId, String driverName,

        String tractorRegistration, String semiTrailerRegistration,

        OffsetDateTime plannedStart, OffsetDateTime plannedEnd,

        String originName, String destinationName,

        boolean emptyTrip,

        double totalCost) {
}