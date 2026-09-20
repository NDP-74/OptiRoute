package com.optiroute.backend.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleEventRequest(LocalDate eventDate, String supplier, BigDecimal cost, Long tractorId, Long semiTrailerId) {
}