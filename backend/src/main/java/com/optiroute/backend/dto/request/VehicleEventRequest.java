package com.optiroute.backend.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record VehicleEventRequest(OffsetDateTime eventDate, String supplier, BigDecimal cost, Long tractorId, Long semiTrailerId) {
}