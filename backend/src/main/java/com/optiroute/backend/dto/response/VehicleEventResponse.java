package com.optiroute.backend.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record VehicleEventResponse(Long id, OffsetDateTime eventDate, String supplier, BigDecimal cost, Long tractorId, String tractorRegistration, Long semiTrailerId,
    String semiTrailerRegistration) {
}