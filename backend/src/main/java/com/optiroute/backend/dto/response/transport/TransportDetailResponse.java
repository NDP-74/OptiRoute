package com.optiroute.backend.dto.response.transport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;

public record TransportDetailResponse(Long id, String name, String status, boolean emptyTrip,

        OffsetDateTime plannedStart, OffsetDateTime plannedEnd,

        OffsetDateTime actualStart, OffsetDateTime actualEnd,

        // Driver
        Long driverId, String driverName, String driverLogin,

        // Tractor
        Long tractorId, String tractorRegistration, String tractorBrand, String tractorModel,

        // Semi-trailer
        Long semiTrailerId, String semiTrailerRegistration, String semiTrailerBrand, String semiTrailerModel,

        // Customer
        Long customerId, String customerName, String customerAddress, String customerCity,

        // Origin
        String originName, String originAddress, double originLat, double originLng,

        // Destination
        String destinationName, String destinationAddress, double destinationLat, double destinationLng,

        // Route estimate
        Long distanceMeters, Long durationSeconds,

        String polyline,

        TransportCostDetailsResponse costs, BigDecimal revenue) {
}
