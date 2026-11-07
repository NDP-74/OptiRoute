package com.optiroute.backend.dto.response.transport;

import java.math.BigDecimal;

import com.optiroute.backend.type.driver.DriverCostType;
import com.optiroute.backend.type.driver.DriverType;

public record PlanningDriverResponse(Long id, String name, String tractorRegistration, String semiTrailerRegistration, BigDecimal salaryForNonTransportDays,
    DriverCostType costType, DriverType driverType) {
}