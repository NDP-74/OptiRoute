package com.optiroute.backend.dto.response.driver;

import com.optiroute.backend.type.driver.DriverCostType;
import com.optiroute.backend.type.driver.DriverType;

public record DriverLightResponse(
        Long id,
        String firstName,
        String lastName,
        DriverCostType costType,
        DriverType driverType) {
}