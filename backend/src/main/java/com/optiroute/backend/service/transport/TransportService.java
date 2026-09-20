package com.optiroute.backend.service.transport;

import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.entity.transport.Transport;
import com.optiroute.backend.repository.transport.TransportRepository;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransportService {

    private final TransportRepository transportRepository;

    public TransportService(TransportRepository transportRepository) {
        this.transportRepository = transportRepository;
    }

    @Transactional
    public Transport create(TransportRequest req) {

        Transport transport = new Transport();
        applyRequest(transport,req);

        return transportRepository.save(transport);
    }

    @Transactional
    public Transport update(Long id, TransportRequest req) {

        Transport transport = transportRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Transport not found with id " + id));

        applyRequest(transport,req);

        return transportRepository.save(transport);
    }

    private void applyRequest(Transport transport, TransportRequest req) {
        transport.setName(req.name());
        transport.setDriverId(req.driverId());
        transport.setTractorId(req.tractorId());
        transport.setSemiTrailerId(req.semiTrailerId());
        transport.setCustomerId(req.customerId());
        transport.setEmptyTrip(req.emptyTrip());

        transport.setPlannedStart(req.plannedStart());
        transport.setPlannedEnd(req.plannedEnd());

        transport.setOriginName(req.originName());
        transport.setOriginAddress(req.originAddress());
        transport.setOriginLat(req.originLat());
        transport.setOriginLng(req.originLng());

        transport.setDestinationName(req.destinationName());
        transport.setDestinationAddress(req.destinationAddress());
        transport.setDestinationLat(req.destinationLat());
        transport.setDestinationLng(req.destinationLng());

        transport.setRevenue(req.revenue());
    }
}