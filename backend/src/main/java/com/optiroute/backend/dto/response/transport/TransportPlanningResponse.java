package com.optiroute.backend.dto.response.transport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransportPlanningResponse(

        Long id, Long routeId, int routeNumber, int routeCount, String name,

        Long driverId, String driverName,

        String tractorRegistration, String semiTrailerRegistration,

        OffsetDateTime departureStart, OffsetDateTime arrivalStart,

        String originName, String destinationName,

        boolean emptyTrip,

        BigDecimal totalCost,

        BigDecimal driverCost, BigDecimal structureCost, BigDecimal vehicleCost,

        BigDecimal revenue) {
}