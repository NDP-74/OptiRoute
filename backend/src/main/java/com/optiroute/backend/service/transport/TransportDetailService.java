package com.optiroute.backend.service.transport;

import org.springframework.stereotype.Service;

import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;
import com.optiroute.backend.dto.response.transport.TransportDetailResponse;
import com.optiroute.backend.entity.Customer;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.transport.Transport;
import com.optiroute.backend.entity.transport.TransportEstimate;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.CustomerRepository;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.TransportEstimateRepository;
import com.optiroute.backend.repository.transport.TransportRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;

import com.optiroute.backend.service.cost.TransportCostService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransportDetailService {

    private final TransportRepository transportRepository;
    private final DriverRepository driverRepository;
    private final TractorRepository tractorRepository;
    private final SemiTrailerRepository semiTrailerRepository;
    private final CustomerRepository customerRepository;
    private final TransportEstimateRepository transportEstimateRepository;

    private final TransportCostService transportCostService;

    public TransportDetailResponse getDetail(Long transportId) {

        Transport transport = transportRepository.findById(transportId).orElseThrow(() -> new RuntimeException("Transport not found"));
        Driver driver = driverRepository.findById(transport.getDriverId()).orElseThrow(() -> new RuntimeException("Driver not found"));

        Tractor tractor = null;
        if (transport.getTractorId() != null) {
            tractor = tractorRepository.findById(transport.getTractorId()).orElse(null);
        }

        SemiTrailer semiTrailer = null;
        if (transport.getSemiTrailerId() != null) {
            semiTrailer = semiTrailerRepository.findById(transport.getSemiTrailerId()).orElse(null);
        }

        Customer customer = null;
        if (transport.getCustomerId() != null) {
            customer = customerRepository.findById(transport.getCustomerId()).orElse(null);
        }

        TransportEstimate estimate = transportEstimateRepository.findByTransportId(transportId).orElse(null);
        TransportCostDetailsResponse costs = transportCostService.calculateCosts(transport,estimate);

        return new TransportDetailResponse(

            transport.getId(), transport.getName(), transport.getStatus(), transport.isEmptyTrip(),

            transport.getPlannedStart(), transport.getPlannedEnd(),

            transport.getActualStart(), transport.getActualEnd(),

            driver.getId(), driver.getFirstName() + " " + driver.getLastName(), driver.getLogin(),

            tractor != null ? tractor.getId() : null, tractor != null ? tractor.getRegistration() : null, tractor != null ? tractor.getBrand() : null,
            tractor != null ? tractor.getModel() : null,

            semiTrailer != null ? semiTrailer.getId() : null, semiTrailer != null ? semiTrailer.getRegistration() : null, semiTrailer != null ? semiTrailer.getBrand() : null,
            semiTrailer != null ? semiTrailer.getModel() : null,

            customer != null ? customer.getId() : null, customer != null ? customer.getName() : null, customer != null ? customer.getAddress() : null,
            customer != null ? customer.getCity() : null,

            transport.getOriginName(), transport.getOriginAddress(), transport.getOriginLat(), transport.getOriginLng(),

            transport.getDestinationName(), transport.getDestinationAddress(), transport.getDestinationLat(), transport.getDestinationLng(),

            estimate != null ? estimate.getDistanceMeters() : null, estimate != null ? estimate.getDurationSeconds() : null,

            estimate != null ? estimate.getPolyline() : null,

            costs, transport.getRevenue());
    }
}
