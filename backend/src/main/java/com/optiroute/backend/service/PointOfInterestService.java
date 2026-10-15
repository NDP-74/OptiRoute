package com.optiroute.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.dto.request.PointOfInterestRequest;
import com.optiroute.backend.dto.response.PointOfInterestResponse;
import com.optiroute.backend.entity.event.PointOfInterest;
import com.optiroute.backend.repository.PointOfInterestRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PointOfInterestService {

    private final PointOfInterestRepository pointOfInterestRepository;

    public PointOfInterestService(PointOfInterestRepository pointOfInterestRepository) {
        this.pointOfInterestRepository = pointOfInterestRepository;
    }

    @Transactional
    public PointOfInterestResponse create(PointOfInterestRequest request) {
        PointOfInterest pointOfInterest = PointOfInterest.builder()
                .label(request.label().trim())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();

        return toResponse(pointOfInterestRepository.save(pointOfInterest));
    }

    @Transactional(readOnly = true)
    public List<PointOfInterestResponse> getAll() {
        return pointOfInterestRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public PointOfInterestResponse update(Long id, PointOfInterestRequest request) {
        PointOfInterest point = pointOfInterestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Point of interest not found with id " + id));

        point.setLabel(request.label().trim());
        point.setAddress(request.address());
        point.setLatitude(request.latitude());
        point.setLongitude(request.longitude());

        return toResponse(pointOfInterestRepository.save(point));
    }

    @Transactional
    public void delete(Long id) {
        PointOfInterest point = pointOfInterestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Point of interest not found with id " + id));

        pointOfInterestRepository.delete(point);
        pointOfInterestRepository.flush();
    }

    private PointOfInterestResponse toResponse(PointOfInterest poi) {
        return new PointOfInterestResponse(poi.getId(), poi.getLabel(), poi.getAddress(), poi.getLatitude(), poi.getLongitude(), poi.getCreatedAt(), poi.getUpdatedAt());
    }
}
