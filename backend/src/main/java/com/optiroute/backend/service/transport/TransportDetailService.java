package com.optiroute.backend.service.transport;

import org.springframework.stereotype.Service;

import com.optiroute.backend.dto.response.transport.TransportDetailResponse;
import com.optiroute.backend.entity.Customer;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.repository.CustomerRepository;
import com.optiroute.backend.repository.transport.RouteRepository;
import com.optiroute.backend.repository.transport.ServiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransportDetailService {

    private final ServiceRepository serviceRepository;
    private final RouteRepository routeRepository;
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final TractorRepository tractorRepository;
    private final SemiTrailerRepository semiTrailerRepository;

    private final EmptyCostDetailsFactory emptyCostDetailsFactory;

    public TransportDetailResponse getDetail(Long serviceId) {

        com.optiroute.backend.entity.transport.Service service = serviceRepository.findById(serviceId).orElseThrow(() -> new RuntimeException("Service not found"));
        Route route = routeRepository.findFirstByServiceId(serviceId).orElseThrow(() -> new RuntimeException("Route not found"));

        Driver driver = route.getDriverId() == null ? null : driverRepository.findById(route.getDriverId()).orElse(null);
        Tractor tractor = route.getTractorId() == null ? null : tractorRepository.findById(route.getTractorId()).orElse(null);
        SemiTrailer semiTrailer = route.getSemiTrailerId() == null ? null : semiTrailerRepository.findById(route.getSemiTrailerId()).orElse(null);

        Customer customer = null;
        if (service.getCustomerId() != null) {
            customer = customerRepository.findById(service.getCustomerId()).orElse(null);
        }

        return new TransportDetailResponse(

            service.getId(), service.getName(), service.getStatus(), route.isEmptyTrip(),

            route.getStartDate(), route.getEndDate(),

            null, null,

            driver != null ? driver.getId() : null, driver != null ? driver.getFirstName() + " " + driver.getLastName() : null, driver != null ? driver.getLogin() : null,

            tractor != null ? tractor.getId() : null, tractor != null ? tractor.getRegistration() : null, tractor != null ? tractor.getBrand() : null,
            tractor != null ? tractor.getModel() : null,

            semiTrailer != null ? semiTrailer.getId() : null, semiTrailer != null ? semiTrailer.getRegistration() : null, semiTrailer != null ? semiTrailer.getBrand() : null,
            semiTrailer != null ? semiTrailer.getModel() : null,

            customer != null ? customer.getId() : null, customer != null ? customer.getName() : null, customer != null ? customer.getAddress() : null,
            customer != null ? customer.getCity() : null,

            route.getOriginName(), route.getOriginAddress(), route.getOriginLat(), route.getOriginLng(),

            route.getDestinationName(), route.getDestinationAddress(), route.getDestinationLat(), route.getDestinationLng(),

            route.getDistanceMeters(), route.getDurationSeconds(),

            route.getPolyline(),

            emptyCostDetailsFactory.create(), service.getRevenue());
    }
}
