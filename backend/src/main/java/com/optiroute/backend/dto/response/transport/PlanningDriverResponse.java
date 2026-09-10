package com.optiroute.backend.dto.response.transport;

import java.math.BigDecimal;

public record PlanningDriverResponse(Long id, String name, String tractorRegistration, String semiTrailerRegistration, BigDecimal salaryForNonTransportDays) {
}