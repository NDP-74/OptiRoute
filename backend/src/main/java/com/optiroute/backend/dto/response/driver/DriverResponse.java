package com.optiroute.backend.dto.response.driver;

import java.math.BigDecimal;

public record DriverResponse(Long id, String login, String firstName, String lastName, String phoneNumber, BigDecimal annualSalary, BigDecimal monthlyWorkingHours, Long tractorId,
    String tractorRegistration, Long semiTrailerId, String semiTrailerRegistration, String costType, String driverType, String locationLabel, BigDecimal locationLatitude,
    BigDecimal locationLongitude) {
}
