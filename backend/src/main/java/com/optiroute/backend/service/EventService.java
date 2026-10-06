package com.optiroute.backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.dto.request.EventRequest;
import com.optiroute.backend.dto.response.EventResponse;
import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.entity.event.Event;
import com.optiroute.backend.entity.event.PointOfInterest;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.EventRepository;
import com.optiroute.backend.repository.PointOfInterestRepository;
import com.optiroute.backend.repository.cost.CostParameterRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.type.cost.CostParameterAssignmentType;

import jakarta.persistence.EntityNotFoundException;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final RouteRepository routeRepository;
    private final CostParameterRepository costParameterRepository;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final TractorRepository tractorRepository;
    private final SemiTrailerRepository semiTrailerRepository;

    public EventService(EventRepository eventRepository, RouteRepository routeRepository, CostParameterRepository costParameterRepository,
            PointOfInterestRepository pointOfInterestRepository, TractorRepository tractorRepository, SemiTrailerRepository semiTrailerRepository) {
        this.eventRepository = eventRepository;
        this.routeRepository = routeRepository;
        this.costParameterRepository = costParameterRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.tractorRepository = tractorRepository;
        this.semiTrailerRepository = semiTrailerRepository;
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        if ((request.tractorId() == null) == (request.semiTrailerId() == null)) {
            throw new IllegalArgumentException("Exactly one of tractor or semi-trailer must be provided");
        }

        Route route = routeRepository.findFirstByServiceId(request.serviceId()).orElseThrow(() -> new EntityNotFoundException("Route not found for service " + request.serviceId()));

        CostParameter costParameter = costParameterRepository.findById(request.costParameterId())
                .orElseThrow(() -> new EntityNotFoundException("Cost parameter not found with id " + request.costParameterId()));

        if (costParameter.getAssignmentType() != CostParameterAssignmentType.MANUAL) {
            throw new IllegalArgumentException("Cost parameter is not manually assignable");
        }

        PointOfInterest pointOfInterest = request.pointOfInterestId() == null ? null
                : pointOfInterestRepository.findById(request.pointOfInterestId()).orElseThrow(() -> new EntityNotFoundException("Point of interest not found with id " + request.pointOfInterestId()));

        Tractor tractor = null;
        SemiTrailer semiTrailer = null;

        if (request.tractorId() != null) {
            if (!request.tractorId().equals(route.getTractorId())) {
                throw new IllegalArgumentException("Tractor does not belong to this route");
            }
            tractor = tractorRepository.findById(request.tractorId()).orElseThrow(() -> new EntityNotFoundException("Tractor not found with id " + request.tractorId()));
        } else {
            if (!request.semiTrailerId().equals(route.getSemiTrailerId())) {
                throw new IllegalArgumentException("Semi-trailer does not belong to this route");
            }
            semiTrailer = semiTrailerRepository.findById(request.semiTrailerId()).orElseThrow(() -> new EntityNotFoundException("Semi-trailer not found with id " + request.semiTrailerId()));
        }

        LocalDateTime eventDate = route.getStartDate() == null ? LocalDateTime.now() : route.getStartDate().toLocalDateTime();

        Event event = Event.builder().route(route).costParameter(costParameter).pointOfInterest(pointOfInterest).tractor(tractor).semiTrailer(semiTrailer).eventDate(eventDate).build();

        Event saved = eventRepository.save(event);

        return new EventResponse(saved.getId(), route.getId(), costParameter.getId(), costParameter.getLabel(), pointOfInterest == null ? null : pointOfInterest.getId(),
                pointOfInterest == null ? null : pointOfInterest.getLabel(), tractor == null ? null : tractor.getId(), semiTrailer == null ? null : semiTrailer.getId(), saved.getEventDate());
    }
}