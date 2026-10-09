package com.optiroute.backend.service.vehicle;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.dto.request.vehicle.SemiTrailerRequest;
import com.optiroute.backend.dto.request.vehicle.TrailerTypeRequest;
import com.optiroute.backend.dto.response.vehicle.SemiTrailerResponse;
import com.optiroute.backend.dto.response.vehicle.SemiTrailerLightResponse;
import com.optiroute.backend.dto.response.vehicle.TrailerTypeResponse;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.TrailerType;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TrailerTypeRepository;
import com.optiroute.backend.utils.VehicleUtils;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SemiTrailerService {

    private final SemiTrailerRepository semiTrailerRepository;
    private final TrailerTypeRepository trailerTypeRepository;

    public SemiTrailerService(SemiTrailerRepository semiTrailerRepository, TrailerTypeRepository trailerTypeRepository) {
        this.semiTrailerRepository = semiTrailerRepository;
        this.trailerTypeRepository = trailerTypeRepository;
    }

    public List<SemiTrailerLightResponse> getAll() {
        return semiTrailerRepository.findAll().stream().map(semiTrailer -> VehicleUtils.toSemiTrailerLightResponse(semiTrailer)).toList();
    }

    @Transactional(readOnly = true)
    public List<TrailerTypeResponse> getAllTrailerTypes() {
        return trailerTypeRepository.findAll(Sort.by("label")).stream().map(type -> toTrailerTypeResponse(type)).toList();
    }

    @Transactional(readOnly = true)
    public TrailerTypeResponse getTrailerType(Long id) {
        return toTrailerTypeResponse(trailerTypeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("TrailerType not found with id " + id)));
    }

    @Transactional
    public TrailerTypeResponse createTrailerType(TrailerTypeRequest request) {
        String label = validateTrailerTypeLabel(request.label());
        if (trailerTypeRepository.findByLabelIgnoreCase(label).isPresent()) {
            throw new IllegalArgumentException("A trailer type with this label already exists");
        }

        TrailerType trailerType = new TrailerType();
        trailerType.setLabel(label);
        trailerType.setDescription(normalizeDescription(request.description()));
        return toTrailerTypeResponse(trailerTypeRepository.save(trailerType));
    }

    @Transactional
    public TrailerTypeResponse updateTrailerType(Long id, TrailerTypeRequest request) {
        String label = validateTrailerTypeLabel(request.label());
        TrailerType trailerType = trailerTypeRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("TrailerType not found with id " + id));

        trailerTypeRepository.findByLabelIgnoreCase(label).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new IllegalArgumentException("A trailer type with this label already exists");
        });

        trailerType.setLabel(label);
        trailerType.setDescription(normalizeDescription(request.description()));
        return toTrailerTypeResponse(trailerTypeRepository.save(trailerType));
    }

    @Transactional(readOnly = true)
    public SemiTrailerResponse getById(Long id) {
        SemiTrailer semiTrailer = semiTrailerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("SemiTrailer not found with id: " + id));

        return VehicleUtils.toSemiTrailerResponse(semiTrailer);
    }

    public SemiTrailer getEntityById(Long id) {
        return semiTrailerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("SemiTrailer not found with id: " + id));
    }

    @Transactional
    public SemiTrailerResponse create(SemiTrailerRequest request) {

        if (semiTrailerRepository.existsByRegistration(request.registration())) {
            throw new IllegalArgumentException("A semiTrailer with registration " + request.registration() + " already exists");
        }

        SemiTrailer semiTrailer = new SemiTrailer();

        semiTrailer.setExternalId(request.externalId());
        semiTrailer.setExternalSource(request.externalSource() == null || request.externalSource().isBlank() ? "MANUAL" : request.externalSource());

        semiTrailer.setRegistration(request.registration());

        semiTrailer.setBrand(request.brand());
        semiTrailer.setModel(request.model());

        semiTrailer.setTrailerType(resolveTrailerType(request.trailerType()));
        semiTrailer.setMaxSpeed(request.maxSpeed());

        semiTrailer.setEmptyWeightKg(request.emptyWeightKg());
        semiTrailer.setGrossVehicleWeightKg(request.grossVehicleWeightKg());

        semiTrailer.setHeightCm(request.heightCm());
        semiTrailer.setWidthCm(request.widthCm());
        semiTrailer.setLengthCm(request.lengthCm());
        semiTrailer.setAxleCount(request.axleCount());

        semiTrailer.setPurchaseCost(request.purchaseCost());
        semiTrailer.setDepreciationStartDate(request.depreciationStartDate());
        semiTrailer.setDepreciationEndDate(request.depreciationEndDate());
        semiTrailer.setMaintenanceCostPerKm(request.maintenanceCostPerKm());

        SemiTrailer savedSemiTrailer = semiTrailerRepository.save(semiTrailer);
        return VehicleUtils.toSemiTrailerResponse(savedSemiTrailer);
    }

    @Transactional
    public SemiTrailerResponse updateSemiTrailer(Long id, SemiTrailerRequest request) {
        SemiTrailer semiTrailer = semiTrailerRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("SemiTrailer not found with id " + id));

        semiTrailer.setExternalId(request.externalId());
        semiTrailer.setExternalSource(request.externalSource() == null || request.externalSource().isBlank() ? "MANUAL" : request.externalSource());

        semiTrailer.setRegistration(request.registration());

        semiTrailer.setBrand(request.brand());
        semiTrailer.setModel(request.model());

        semiTrailer.setTrailerType(resolveTrailerType(request.trailerType()));
        semiTrailer.setMaxSpeed(request.maxSpeed());

        semiTrailer.setEmptyWeightKg(request.emptyWeightKg());
        semiTrailer.setGrossVehicleWeightKg(request.grossVehicleWeightKg());

        semiTrailer.setHeightCm(request.heightCm());
        semiTrailer.setWidthCm(request.widthCm());
        semiTrailer.setLengthCm(request.lengthCm());
        semiTrailer.setAxleCount(request.axleCount());

        semiTrailer.setPurchaseCost(request.purchaseCost());
        semiTrailer.setDepreciationStartDate(request.depreciationStartDate());
        semiTrailer.setDepreciationEndDate(request.depreciationEndDate());
        semiTrailer.setMaintenanceCostPerKm(request.maintenanceCostPerKm());

        SemiTrailer updatedSemiTrailer = semiTrailerRepository.save(semiTrailer);
        return VehicleUtils.toSemiTrailerResponse(updatedSemiTrailer);
    }

    @Transactional
    public void deleteSemiTrailer(Long id) {
        if (!semiTrailerRepository.existsById(id)) {
            throw new EntityNotFoundException("SemiTrailer not found with id " + id);
        }

        semiTrailerRepository.deleteById(id);
    }

    @Transactional
    public void deleteTrailerType(Long id) {
        if (!trailerTypeRepository.existsById(id)) {
            throw new EntityNotFoundException("TrailerType not found with id " + id);
        }

        trailerTypeRepository.deleteById(id);
    }

    private TrailerTypeResponse toTrailerTypeResponse(TrailerType trailerType) {
        return new TrailerTypeResponse(trailerType.getId(), trailerType.getLabel(), trailerType.getDescription());
    }

    private String validateTrailerTypeLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Trailer type label is required");
        }
        return label.trim();
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private TrailerType resolveTrailerType(String requestedLabel) {
        if (requestedLabel == null || requestedLabel.isBlank()) {
            return null;
        }

        return trailerTypeRepository.findByLabelIgnoreCase(requestedLabel).orElseGet(() -> {
            TrailerType trailerType = new TrailerType();
            trailerType.setLabel(requestedLabel);
            return trailerTypeRepository.save(trailerType);
        });
    }

}