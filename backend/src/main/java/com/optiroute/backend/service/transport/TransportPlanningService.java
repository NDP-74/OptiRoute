package com.optiroute.backend.service.transport;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;
import com.optiroute.backend.dto.response.transport.PlanningDriverResponse;
import com.optiroute.backend.dto.response.transport.PlanningResponse;
import com.optiroute.backend.dto.response.transport.PlanningUnassignedVehiclesResponse;
import com.optiroute.backend.dto.response.transport.TransportPlanningResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.transport.TransportEstimate;
import com.optiroute.backend.entity.transport.Transport;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.TransportEstimateRepository;
import com.optiroute.backend.repository.transport.TransportRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.service.cost.TransportCostService;
import com.optiroute.backend.service.cost.DriverCostService;
import com.optiroute.backend.service.cost.VehicleCostService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransportPlanningService {

    private final TransportRepository transportRepository;
    private final DriverRepository driverRepository;
    private final TransportEstimateRepository transportEstimateRepository;
    private final TransportCostService transportCostService;
    private final DriverCostService driverCostService;
    private final TractorRepository tractorRepository;
    private final SemiTrailerRepository semiTrailerRepository;
    private final VehicleCostService vehicleCostService;

    // Récupére le planning des transports pour une plage de date
    private static final ZoneId PLANNING_ZONE = ZoneId.of("Europe/Paris");

    public PlanningResponse getPlanning(LocalDate startDate, LocalDate endDate) {

        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        OffsetDateTime start = startDate.atStartOfDay(PLANNING_ZONE).toOffsetDateTime();
        OffsetDateTime end = endDate.atStartOfDay(PLANNING_ZONE).toOffsetDateTime();

        List<Transport> transportEntities = transportRepository.findByPlannedStartGreaterThanEqualAndPlannedStartLessThan(start,end);

        List<TransportPlanningResponse> transports = transportEntities.stream().map(transport -> {
            Driver driver = driverRepository.findById(transport.getDriverId()).orElseThrow();
            TransportEstimate estimate = transportEstimateRepository.findByTransportId(transport.getId()).orElse(null);
            TransportCostDetailsResponse costs = transportCostService.calculateCosts(transport,estimate);

            String tractorRegistration = transport.getTractorId() == null ? null : tractorRepository.findById(transport.getTractorId()).map(Tractor::getRegistration).orElse(null);
            String semiTrailerRegistration = transport.getSemiTrailerId() == null ? null
                : semiTrailerRepository.findById(transport.getSemiTrailerId()).map(SemiTrailer::getRegistration).orElse(null);

            return new TransportPlanningResponse(transport.getId(), transport.getName(), driver.getId(), driver.getFirstName() + " " + driver.getLastName(), tractorRegistration,
                semiTrailerRegistration, transport.getPlannedStart(), transport.getPlannedEnd(), transport.getOriginName(), transport.getDestinationName(), transport.isEmptyTrip(),
                costs.totalCost());
        }).toList();

        Map<Long, Set<LocalDate>> transportDatesByDriver = transportEntities.stream().collect(Collectors.groupingBy(Transport::getDriverId,
            Collectors.mapping(transport -> transport.getPlannedStart().atZoneSameInstant(PLANNING_ZONE).toLocalDate(),Collectors.toSet())));

        List<Driver> allDrivers = driverRepository.findAll();

        List<PlanningDriverResponse> drivers = allDrivers.stream()
            .map(driver -> new PlanningDriverResponse(driver.getId(), driver.getFirstName() + " " + driver.getLastName(),
                driver.getTractor() != null ? driver.getTractor().getRegistration() : null, driver.getSemiTrailer() != null ? driver.getSemiTrailer().getRegistration() : null,
                driverCostService.calculateSalaryForPeriodExcludingDates(driver,startDate,endDate,transportDatesByDriver.getOrDefault(driver.getId(),Set.of()))))
            .toList();

        PlanningUnassignedVehiclesResponse unassignedVehicles = buildUnassignedVehicles(allDrivers,startDate,endDate);

        return new PlanningResponse(transports, drivers, unassignedVehicles);
    }

    // Véhicules non affectés à un conducteur, avec leur coût d'amortissement cumulé
    // pour la période sélectionnée
    private PlanningUnassignedVehiclesResponse buildUnassignedVehicles(List<Driver> allDrivers, LocalDate startDate, LocalDate endDate) {
        Set<Long> assignedTractorIds = allDrivers.stream().map(Driver::getTractor).filter(java.util.Objects::nonNull).map(Tractor::getId).collect(Collectors.toSet());
        Set<Long> assignedSemiTrailerIds = allDrivers.stream().map(Driver::getSemiTrailer).filter(java.util.Objects::nonNull).map(SemiTrailer::getId).collect(Collectors.toSet());

        List<Tractor> unassignedTractors = tractorRepository.findAll().stream().filter(tractor -> !assignedTractorIds.contains(tractor.getId())).toList();
        List<SemiTrailer> unassignedSemiTrailers = semiTrailerRepository.findAll().stream().filter(semiTrailer -> !assignedSemiTrailerIds.contains(semiTrailer.getId())).toList();

        List<String> registrations = new java.util.ArrayList<>();
        registrations.addAll(unassignedTractors.stream().map(Tractor::getRegistration).toList());
        registrations.addAll(unassignedSemiTrailers.stream().map(SemiTrailer::getRegistration).toList());

        double depreciationCost = unassignedTractors.stream()
            .mapToDouble(tractor -> vehicleCostService.calculateDepreciationForPeriod(tractor.getPurchaseCost(),tractor.getDepreciationStartDate(),tractor.getDepreciationEndDate(),
                startDate,endDate))
            .sum()
            + unassignedSemiTrailers.stream().mapToDouble(semiTrailer -> vehicleCostService.calculateDepreciationForPeriod(semiTrailer.getPurchaseCost(),
                semiTrailer.getDepreciationStartDate(),semiTrailer.getDepreciationEndDate(),startDate,endDate)).sum();

        return new PlanningUnassignedVehiclesResponse(registrations, depreciationCost);
    }

}