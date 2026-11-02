package com.optiroute.backend.dto.response.transport;

import java.time.OffsetDateTime;

public record TransportPlanningResponse(

        Long id, Long routeId, int routeNumber, int routeCount, String name,

        Long driverId, String driverName,

        String tractorRegistration, String semiTrailerRegistration,

        OffsetDateTime departureStart, OffsetDateTime departureEnd,
        OffsetDateTime arrivalStart, OffsetDateTime arrivalEnd,

        String originName, String destinationName,

        boolean emptyTrip,

        double totalCost,

        double driverCost, double structureCost, double vehicleCost,

        double revenue) {
}