package com.optiroute.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleEventResponse(Long id, LocalDate eventDate, String supplier, BigDecimal cost, Long tractorId, String tractorRegistration, Long semiTrailerId,
    String semiTrailerRegistration) {
}