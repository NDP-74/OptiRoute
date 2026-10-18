package com.optiroute.backend.dto.request.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SemiTrailerRequest(String registration, String trailerType, BigDecimal purchaseCost, LocalDate depreciationStartDate, LocalDate depreciationEndDate,
        BigDecimal maintenanceCostPerKm) {
}
