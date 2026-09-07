package com.optiroute.backend.utils;

import com.optiroute.backend.dto.response.driver.DriverResponse;
import com.optiroute.backend.dto.response.driver.DriverLightResponse;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.entity.driver.Driver;

public class DriverUtils {
    public static DriverResponse toDriverResponse(Driver driver) {
        return new DriverResponse(driver.getId(), driver.getLogin(), driver.getFirstName(), driver.getLastName(), driver.getPhoneNumber(), driver.getAnnualSalary(),
            driver.getMonthlyWorkingHours(), driver.getTractor() != null ? driver.getTractor().getId() : null,
            driver.getTractor() != null ? driver.getTractor().getRegistration() : null, driver.getSemiTrailer() != null ? driver.getSemiTrailer().getId() : null,
            driver.getSemiTrailer() != null ? driver.getSemiTrailer().getRegistration() : null, driver.getCostType() != null ? driver.getCostType().name() : null,
            driver.getDriverType() != null ? driver.getDriverType().name() : null, driver.getLocationLabel(), driver.getLocationLatitude(), driver.getLocationLongitude());
    }

    public static DriverLightResponse toSummaryResponse(Driver driver) {
        return new DriverLightResponse(driver.getId(), driver.getFirstName(), driver.getLastName());
    }

    public static String generateUniqueLogin(DriverRepository driverRepository, String firstName, String lastName) {
        String normalizedFirstName = CommonUtils.normalizeText(firstName).toUpperCase();
        String normalizedLastName = CommonUtils.normalizeText(lastName).toUpperCase();

        String baseLogin = normalizedFirstName.substring(0,1) + normalizedLastName;
        String login = baseLogin;
        int suffix = 2;

        while (driverRepository.existsByLogin(login)) {
            login = baseLogin + suffix;
            suffix++;
        }

        return login;
    }
}
