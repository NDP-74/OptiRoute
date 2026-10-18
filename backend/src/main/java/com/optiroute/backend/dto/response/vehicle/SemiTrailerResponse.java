package com.optiroute.backend.dto.response.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SemiTrailerResponse(Long id, String registration, String trailerType, BigDecimal purchaseCost, LocalDate depreciationStartDate, LocalDate depreciationEndDate,
        BigDecimal maintenanceCostPerKm) {
}