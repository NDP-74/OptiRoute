package com.optiroute.backend.service.vehicle;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.dto.request.vehicle.TractorRequest;

import com.optiroute.backend.dto.response.vehicle.TractorResponse;
import com.optiroute.backend.dto.response.vehicle.TractorLightResponse;

import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.utils.VehicleUtils;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TractorService {

    private final TractorRepository tractorRepository;

    public TractorService(TractorRepository tractorRepository) {
        this.tractorRepository = tractorRepository;
    }

    @Transactional(readOnly = true)
    public List<TractorLightResponse> getAll() {
        return tractorRepository.findAll().stream().map(tractor -> VehicleUtils.toTractorLightResponse(tractor)).toList();
    }

    @Transactional(readOnly = true)
    public TractorResponse getById(Long id) {
        Tractor tractor = tractorRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tractor not found with id: " + id));

        return VehicleUtils.toTractorResponse(tractor);
    }

    public Tractor getEntityById(Long id) {
        return tractorRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tractor not found with id: " + id));
    }

    @Transactional
    public TractorResponse create(TractorRequest request) {

        if (tractorRepository.existsByRegistration(request.registration())) {
            throw new IllegalArgumentException("A tractor with registration " + request.registration() + " already exists");
        }

        Tractor tractor = new Tractor();

        tractor.setExternalId(request.externalId());
        tractor.setExternalSource(request.externalSource() == null || request.externalSource().isBlank() ? "MANUAL" : request.externalSource());

        tractor.setRegistration(request.registration());

        tractor.setBrand(request.brand());
        tractor.setModel(request.model());

        tractor.setMaxSpeed(request.maxSpeed());

        tractor.setFuelType(request.fuelType());
        tractor.setAverageConsumption(request.averageConsumption());
        tractor.setAverageConsumptionEmpty(request.averageConsumptionEmpty());

        tractor.setEmptyWeightKg(request.emptyWeightKg());
        tractor.setGrossCombinationWeightKg(request.grossCombinationWeightKg());

        tractor.setHeightCm(request.heightCm());
        tractor.setWidthCm(request.widthCm());
        tractor.setLengthCm(request.lengthCm());
        tractor.setAxleCount(request.axleCount());

        tractor.setPurchaseCost(request.purchaseCost());
        tractor.setDepreciationStartDate(request.depreciationStartDate());
        tractor.setDepreciationEndDate(request.depreciationEndDate());
        tractor.setMaintenanceCostPerKm(request.maintenanceCostPerKm());

        Tractor savedTractor = tractorRepository.save(tractor);
        return VehicleUtils.toTractorResponse(savedTractor);
    }

    @Transactional
    public TractorResponse updateTractor(Long id, TractorRequest request) {
        Tractor tractor = tractorRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Tractor not found with id " + id));

        tractor.setExternalId(request.externalId());
        tractor.setExternalSource(request.externalSource() == null || request.externalSource().isBlank() ? "MANUAL" : request.externalSource());

        tractor.setRegistration(request.registration());

        tractor.setBrand(request.brand());
        tractor.setModel(request.model());

        tractor.setMaxSpeed(request.maxSpeed());

        tractor.setFuelType(request.fuelType());
        tractor.setAverageConsumption(request.averageConsumption());
        tractor.setAverageConsumptionEmpty(request.averageConsumptionEmpty());

        tractor.setEmptyWeightKg(request.emptyWeightKg());
        tractor.setGrossCombinationWeightKg(request.grossCombinationWeightKg());

        tractor.setHeightCm(request.heightCm());
        tractor.setWidthCm(request.widthCm());
        tractor.setLengthCm(request.lengthCm());
        tractor.setAxleCount(request.axleCount());

        tractor.setPurchaseCost(request.purchaseCost());
        tractor.setDepreciationStartDate(request.depreciationStartDate());
        tractor.setDepreciationEndDate(request.depreciationEndDate());
        tractor.setMaintenanceCostPerKm(request.maintenanceCostPerKm());

        Tractor updatedTractor = tractorRepository.save(tractor);
        return VehicleUtils.toTractorResponse(updatedTractor);
    }

    @Transactional
    public void deleteTractor(Long id) {
        if (!tractorRepository.existsById(id)) {
            throw new EntityNotFoundException("Tractor not found with id " + id);
        }

        tractorRepository.deleteById(id);
    }
}