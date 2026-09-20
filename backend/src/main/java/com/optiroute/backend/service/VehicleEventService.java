package com.optiroute.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.dto.request.VehicleEventRequest;
import com.optiroute.backend.dto.response.VehicleEventResponse;
import com.optiroute.backend.entity.VehicleEvent;
import com.optiroute.backend.service.vehicle.SemiTrailerService;
import com.optiroute.backend.service.vehicle.TractorService;
import com.optiroute.backend.repository.VehicleEventRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class VehicleEventService {

    private final VehicleEventRepository vehicleEventRepository;
    private final TractorService tractorService;
    private final SemiTrailerService semiTrailerService;

    public VehicleEventService(VehicleEventRepository vehicleEventRepository, TractorService tractorService, SemiTrailerService semiTrailerService) {
        this.vehicleEventRepository = vehicleEventRepository;
        this.tractorService = tractorService;
        this.semiTrailerService = semiTrailerService;
    }

    @Transactional(readOnly = true)
    public VehicleEventResponse getById(Long id) {
        return toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<VehicleEventResponse> getByDateRange(LocalDate startDate, LocalDate endDate) {
        return vehicleEventRepository.findByEventDateBetween(startDate,endDate).stream().map(this::toResponse).toList();
    }

    @Transactional
    public VehicleEventResponse create(VehicleEventRequest request) {
        VehicleEvent vehicleEvent = new VehicleEvent();
        applyRequest(vehicleEvent,request);

        return toResponse(vehicleEventRepository.save(vehicleEvent));
    }

    @Transactional
    public VehicleEventResponse update(Long id, VehicleEventRequest request) {
        VehicleEvent vehicleEvent = getEntityById(id);
        applyRequest(vehicleEvent,request);

        return toResponse(vehicleEventRepository.save(vehicleEvent));
    }

    @Transactional
    public void delete(Long id) {
        if (!vehicleEventRepository.existsById(id)) {
            throw new EntityNotFoundException("Vehicle event not found with id " + id);
        }

        vehicleEventRepository.deleteById(id);
    }

    private VehicleEvent getEntityById(Long id) {
        return vehicleEventRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Vehicle event not found with id " + id));
    }

    private void applyRequest(VehicleEvent vehicleEvent, VehicleEventRequest request) {
        if (request.eventDate() == null) {
            throw new IllegalArgumentException("Event date is required");
        }

        if (request.cost() == null || request.cost().signum() < 0) {
            throw new IllegalArgumentException("Event cost must be positive or zero");
        }

        if ((request.tractorId() == null) == (request.semiTrailerId() == null)) {
            throw new IllegalArgumentException("An event must be associated with either a tractor or a semi-trailer");
        }

        vehicleEvent.setEventDate(request.eventDate());
        vehicleEvent.setSupplier(request.supplier());
        vehicleEvent.setCost(request.cost());
        vehicleEvent.setTractor(request.tractorId() == null ? null : tractorService.getEntityById(request.tractorId()));
        vehicleEvent.setSemiTrailer(request.semiTrailerId() == null ? null : semiTrailerService.getEntityById(request.semiTrailerId()));
    }

    private VehicleEventResponse toResponse(VehicleEvent vehicleEvent) {
        return new VehicleEventResponse(vehicleEvent.getId(), vehicleEvent.getEventDate(), vehicleEvent.getSupplier(), vehicleEvent.getCost(),
            vehicleEvent.getTractor() == null ? null : vehicleEvent.getTractor().getId(), vehicleEvent.getTractor() == null ? null : vehicleEvent.getTractor().getRegistration(),
            vehicleEvent.getSemiTrailer() == null ? null : vehicleEvent.getSemiTrailer().getId(),
            vehicleEvent.getSemiTrailer() == null ? null : vehicleEvent.getSemiTrailer().getRegistration());
    }
}