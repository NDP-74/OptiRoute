package com.optiroute.backend.dto.response.transport;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;

import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.type.GpsModeType;

public record TransportRouteDetailResponse(
    Long routeId,
    int routeNumber,
    int routeCount,
    boolean emptyTrip,
    String originName,
    String originAddress,
    double originLat,
    double originLng,
    String destinationName,
    String destinationAddress,
    double destinationLat,
    double destinationLng,
    Long distanceMeters,
    Long durationSeconds,
    String polyline,
    GpsModeType routeType,
    OffsetDateTime departureStart,
    OffsetDateTime departureEnd,
    OffsetDateTime arrivalStart,
    OffsetDateTime arrivalEnd,
    List<Position> waypoints,
    TransportCostDetailsResponse costs,
    BigDecimal revenue) {
}
