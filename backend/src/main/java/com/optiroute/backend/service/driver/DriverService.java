package com.optiroute.backend.service.driver;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.dto.request.driver.DriverRequest;
import com.optiroute.backend.dto.response.driver.DriverResponse;
import com.optiroute.backend.dto.response.driver.DriverLightResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.service.vehicle.SemiTrailerService;
import com.optiroute.backend.service.vehicle.TractorService;

import com.optiroute.backend.utils.DriverUtils;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final TractorService tractorService;
    private final SemiTrailerService semiTrailerService;

    public DriverService(DriverRepository driverRepository, PasswordEncoder passwordEncoder, TractorService tractorService, SemiTrailerService semiTrailerService) {
        this.driverRepository = driverRepository;
        this.tractorService = tractorService;
        this.semiTrailerService = semiTrailerService;
    }

    public List<DriverLightResponse> getAll() {
        return driverRepository.findAll().stream().map(driver -> DriverUtils.toSummaryResponse(driver)).toList();
    }

    @Transactional(readOnly = true)
    public DriverResponse getById(Long id) {
        Driver driver = driverRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + id));

        return DriverUtils.toDriverResponse(driver);
    }

    public Driver getEntityById(Long id) {
        return driverRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + id));
    }

    @Transactional
    public DriverResponse createDriver(DriverRequest request) {
        Driver driver = new Driver();
        driver.setLogin(DriverUtils.generateUniqueLogin(driverRepository,request.firstName(),request.lastName()));
        driver.setFirstName(request.firstName());
        driver.setLastName(request.lastName());
        driver.setPhoneNumber(request.phoneNumber());
        driver.setAnnualSalary(request.annualSalary());
        driver.setMonthlyWorkingHours(request.monthlyWorkingHours());
        applyLocation(driver,request);
        applyVehicleAndTypes(driver,request);

        Driver savedDriver = driverRepository.save(driver);
        return DriverUtils.toDriverResponse(savedDriver);
    }

    @Transactional
    public DriverResponse updateDriver(Long id, DriverRequest request) {
        Driver driver = driverRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Driver not found with id " + id));
        driver.setFirstName(request.firstName());
        driver.setLastName(request.lastName());
        driver.setLogin(DriverUtils.generateUniqueLogin(driverRepository,request.firstName(),request.lastName()));
        driver.setPhoneNumber(request.phoneNumber());
        driver.setAnnualSalary(request.annualSalary());
        driver.setMonthlyWorkingHours(request.monthlyWorkingHours());
        applyLocation(driver,request);
        applyVehicleAndTypes(driver,request);

        Driver updatedDriver = driverRepository.save(driver);
        return DriverUtils.toDriverResponse(updatedDriver);
    }

    private void applyVehicleAndTypes(Driver driver, DriverRequest request) {
        driver.setTractor(request.tractorId() == null ? null : tractorService.getEntityById(request.tractorId()));
        driver.setSemiTrailer(request.semiTrailerId() == null ? null : semiTrailerService.getEntityById(request.semiTrailerId()));
        driver.setCostType(request.costType());
        driver.setDriverType(request.driverType());
    }

    private void applyLocation(Driver driver, DriverRequest request) {
        driver.setLocationLabel(request.locationLabel());
        driver.setLocationLatitude(request.locationLatitude());
        driver.setLocationLongitude(request.locationLongitude());
    }

    @Transactional
    public void deleteDriver(Long id) {
        if (!driverRepository.existsById(id)) {
            throw new EntityNotFoundException("Driver not found with id " + id);
        }

        driverRepository.deleteById(id);
    }
}