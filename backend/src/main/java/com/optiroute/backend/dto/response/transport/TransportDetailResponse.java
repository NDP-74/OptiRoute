package com.optiroute.backend.dto.response.transport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.optiroute.backend.dto.request.transport.ServiceTiming;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;

public record TransportDetailResponse(Long id, String name, String status, boolean emptyTrip,

        // Driver
        Long driverId, String driverName, String driverLogin,

        // Tractor
        Long tractorId, String tractorRegistration,

        // Semi-trailer
        Long semiTrailerId, String semiTrailerRegistration,

        // Customer
        Long customerId, String customerName, String customerAddress,

        // Origin
        String originName, String originAddress, double originLat, double originLng,

        // Destination
        String destinationName, String destinationAddress, double destinationLat, double destinationLng,

        // Route estimate
        Long distanceMeters, Long durationSeconds,

        String polyline, GpsModeType routeType,

        TransportCostDetailsResponse costs, BigDecimal revenue,

        ServiceTiming serviceTiming,
        OffsetDateTime departureStart, OffsetDateTime departureEnd,
        OffsetDateTime arrivalStart, OffsetDateTime arrivalEnd,
        List<Position> waypoints) {
}
