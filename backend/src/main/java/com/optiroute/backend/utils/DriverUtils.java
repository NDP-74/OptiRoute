package com.optiroute.backend.utils;

import com.optiroute.backend.dto.response.driver.DriverResponse;
import com.optiroute.backend.dto.response.driver.DriverLightResponse;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.entity.driver.Driver;

public class DriverUtils {
    public static DriverResponse toDriverResponse(Driver driver) {
        return new DriverResponse(driver.getId(), driver.getLogin(), driver.getFirstName(), driver.getLastName(), driver.getAnnualSalary(), driver.getMonthlyWorkingHours(),
            driver.getTractor() != null ? driver.getTractor().getId() : null, driver.getTractor() != null ? driver.getTractor().getRegistration() : null,
            driver.getSemiTrailer() != null ? driver.getSemiTrailer().getId() : null, driver.getSemiTrailer() != null ? driver.getSemiTrailer().getRegistration() : null,
            driver.getCostType() != null ? driver.getCostType().name() : null, driver.getDriverType() != null ? driver.getDriverType().name() : null, driver.getLocationLabel(),
            driver.getLocationLatitude(), driver.getLocationLongitude());
    }

    public static DriverLightResponse toSummaryResponse(Driver driver) {
        return new DriverLightResponse(
            driver.getId(),
            driver.getFirstName(),
            driver.getLastName(),
            driver.getCostType(),
            driver.getDriverType()
        );
    }

    public static String generateUniqueLogin(DriverRepository driverRepository, String firstName, String lastName) {
        return generateUniqueLogin(driverRepository, firstName, lastName, null);
    }

    /**
     * Génère un login unique. {@code excludedDriverId} permet d'ignorer le conducteur en cours de modification.
     */
    public static String generateUniqueLogin(DriverRepository driverRepository, String firstName, String lastName,
            Long excludedDriverId) {
        String baseLogin = baseLogin(firstName, lastName);
        String login = baseLogin;
        int suffix = 2;

        while (loginTaken(driverRepository, login, excludedDriverId)) {
            login = baseLogin + suffix;
            suffix++;
        }

        return login;
    }

    /**
     * Indique si le login correspond déjà aux nom et prénom (login de base, éventuellement suffixé d'un numéro).
     */
    public static boolean loginMatchesName(String login, String firstName, String lastName) {
        if (login == null) {
            return false;
        }
        String baseLogin = baseLogin(firstName, lastName);
        return login.equals(baseLogin)
            || (login.startsWith(baseLogin) && login.substring(baseLogin.length()).matches("\\d+"));
    }

    private static String baseLogin(String firstName, String lastName) {
        String normalizedFirstName = CommonUtils.normalizeText(firstName).toUpperCase();
        String normalizedLastName = CommonUtils.normalizeText(lastName).toUpperCase();
        String initial = normalizedFirstName.isEmpty() ? "" : normalizedFirstName.substring(0, 1);
        String baseLogin = initial + normalizedLastName;

        return baseLogin.isEmpty() ? "DRIVER" : baseLogin;
    }

    private static boolean loginTaken(DriverRepository driverRepository, String login, Long excludedDriverId) {
        return excludedDriverId == null
            ? driverRepository.existsByLogin(login)
            : driverRepository.existsByLoginAndIdNot(login, excludedDriverId);
    }
}
