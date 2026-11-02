package com.optiroute.backend.service.transport;

import java.util.List;
import java.util.function.Function;
import java.math.BigDecimal;
import com.optiroute.backend.dto.request.transport.ServiceTiming;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import com.optiroute.backend.dto.response.transport.TransportDetailResponse;
import com.optiroute.backend.dto.response.transport.TransportRouteDetailResponse;
import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;
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

    private final TransportCostService transportCostService;

    public TransportDetailResponse getDetail(Long serviceId) {
        return getDetail(serviceId, null);
    }

    public TransportDetailResponse getDetail(Long serviceId, Long routeId) {
        com.optiroute.backend.entity.transport.Service service = serviceRepository.findById(serviceId).orElseThrow(() -> new RuntimeException("Service not found"));
        List<Route> allServiceRoutes = routeRepository.findByServiceIdOrderByIdAsc(serviceId);
        if (allServiceRoutes.isEmpty()) {
            throw new RuntimeException("Route not found");
        }
        int routeNumber = 1;
        List<Route> routes = allServiceRoutes;
        if (routeId != null) {
            routeNumber = -1;
            for (int index = 0; index < allServiceRoutes.size(); index++) {
                if (allServiceRoutes.get(index).getId().equals(routeId)) {
                    routeNumber = index + 1;
                    break;
                }
            }
            if (routeNumber < 1) {
                throw new EntityNotFoundException("Route " + routeId + " not found for service " + serviceId);
            }
            routes = List.of(allServiceRoutes.get(routeNumber - 1));
        }
        Route route = routes.getFirst();

        Driver driver = route.getDriverId() == null ? null : driverRepository.findById(route.getDriverId()).orElse(null);
        Tractor tractor = route.getTractorId() == null ? null : tractorRepository.findById(route.getTractorId()).orElse(null);
        SemiTrailer semiTrailer = route.getSemiTrailerId() == null ? null : semiTrailerRepository.findById(route.getSemiTrailerId()).orElse(null);

        Customer customer = null;
        if (service.getCustomerId() != null) {
            customer = customerRepository.findById(service.getCustomerId()).orElse(null);
        }
        List<TransportRouteDetailResponse> routeDetails = routes.stream().map(this::toRouteDetail).toList();
        TransportCostDetailsResponse costs = routeId == null
            ? aggregateCosts(routeDetails)
            : routeDetails.getFirst().costs();
        BigDecimal revenue = ServiceRouteRevenueAllocator.allocate(service.getRevenue(), allServiceRoutes, route);

        return new TransportDetailResponse(

            service.getId(), route.getId(), routeNumber, allServiceRoutes.size(),
            service.getName(), service.getStatus(), route.isEmptyTrip(),

            driver != null ? driver.getId() : null, driver != null ? driver.getFirstName() + " " + driver.getLastName() : null, driver != null ? driver.getLogin() : null,

            tractor != null ? tractor.getId() : null, tractor != null ? tractor.getRegistration() : null,

            semiTrailer != null ? semiTrailer.getId() : null, semiTrailer != null ? semiTrailer.getRegistration() : null,

            customer != null ? customer.getId() : null, customer != null ? customer.getName() : null, customer != null ? customer.getAddress() : null,

            route.getOriginName(), route.getOriginAddress(), route.getOriginLat(), route.getOriginLng(),

            route.getDestinationName(), route.getDestinationAddress(), route.getDestinationLat(), route.getDestinationLng(),

            route.getDistanceMeters(), route.getDurationSeconds(),

            route.getPolyline(), route.getRouteType(),

            costs, revenue,
            new ServiceTiming(service.getLoadingDurationMinutes(), service.getUnloadingDurationMinutes(),
                service.getDepartureStart(), service.getDepartureEnd(), service.getArrivalStart(), service.getArrivalEnd()),
            route.getDepartureStart(), route.getDepartureEnd(), route.getArrivalStart(), route.getArrivalEnd(),
            route.getWaypoints() == null ? List.of() : route.getWaypoints(),
            routeDetails);
    }

    private TransportRouteDetailResponse toRouteDetail(Route route) {
        return new TransportRouteDetailResponse(route.getId(), route.isEmptyTrip(), route.getOriginName(),
            route.getOriginAddress(), route.getOriginLat(), route.getOriginLng(), route.getDestinationName(),
            route.getDestinationAddress(), route.getDestinationLat(), route.getDestinationLng(),
            route.getDistanceMeters(), route.getDurationSeconds(), route.getPolyline(), route.getRouteType(),
            route.getDepartureStart(), route.getDepartureEnd(), route.getArrivalStart(), route.getArrivalEnd(),
            route.getWaypoints() == null ? List.of() : route.getWaypoints(), transportCostService.calculate(route));
    }

    private TransportCostDetailsResponse aggregateCosts(List<TransportRouteDetailResponse> routes) {
        List<TransportCostDetailsResponse> costs = routes.stream().map(TransportRouteDetailResponse::costs).toList();
        CostCategoryResponse vehicle = aggregateCategory(costs, TransportCostDetailsResponse::vehicle);
        CostCategoryResponse driver = aggregateCategory(costs, TransportCostDetailsResponse::driver);
        CostCategoryResponse structure = aggregateCategory(costs, TransportCostDetailsResponse::structure);
        double total = costs.stream().mapToDouble(TransportCostDetailsResponse::totalCost).sum();
        return new TransportCostDetailsResponse(vehicle, driver, structure, total);
    }

    private CostCategoryResponse aggregateCategory(List<TransportCostDetailsResponse> routeCosts,
        Function<TransportCostDetailsResponse, CostCategoryResponse> categoryExtractor) {
        List<CostCategoryResponse> categories = routeCosts.stream().map(categoryExtractor).toList();
        return new CostCategoryResponse(categories.stream().flatMap(category -> category.costs().stream()).toList(),
            categories.stream().mapToDouble(CostCategoryResponse::totalCost).sum());
    }
}
