package com.optiroute.backend.dto.response.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TractorResponse(Long id, String registration, BigDecimal purchaseCost, LocalDate depreciationStartDate, LocalDate depreciationEndDate,
        BigDecimal maintenanceCostPerKm) {
}
