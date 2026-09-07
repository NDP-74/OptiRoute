package com.optiroute.backend.dto.request.driver;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import com.optiroute.backend.type.driver.DriverCostType;
import com.optiroute.backend.type.driver.DriverType;

public record DriverRequest(@NotBlank String firstName, @NotBlank String lastName, String phoneNumber, @NotNull @PositiveOrZero BigDecimal annualSalary,
        @NotNull @Positive BigDecimal monthlyWorkingHours, Long tractorId, Long semiTrailerId, @NotNull DriverCostType costType, @NotNull DriverType driverType,
        String locationLabel, BigDecimal locationLatitude, BigDecimal locationLongitude) {
}