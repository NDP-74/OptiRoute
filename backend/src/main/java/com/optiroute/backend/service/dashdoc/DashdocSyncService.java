package com.optiroute.backend.service.dashdoc;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.optiroute.backend.client.DashdocClient;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.transport.DashdocSyncResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.entity.transport.Service;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.exception.RoutingProviderException;
import com.optiroute.backend.exception.TransportPlanningException;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.ServiceRepository;
import com.optiroute.backend.repository.vehicle.SemiTrailerRepository;
import com.optiroute.backend.repository.vehicle.TractorRepository;
import com.optiroute.backend.service.transport.TransportFacadeService;

import lombok.extern.slf4j.Slf4j;

@org.springframework.stereotype.Service
@Slf4j
public class DashdocSyncService {

    private static final String DASHDOC_SOURCE = "DASHDOC";
    private static final int DEFAULT_HANDLING_DURATION_MINUTES = 45;

    private final DashdocClient dashdocClient;
    private final DashdocTransportMapper transportMapper;
    private final DriverRepository driverRepository;
    private final TractorRepository tractorRepository;
    private final SemiTrailerRepository semiTrailerRepository;
    private final ServiceRepository serviceRepository;
    private final TransportFacadeService transportFacadeService;

    public DashdocSyncService(
            DashdocClient dashdocClient,
            DashdocTransportMapper transportMapper,
            DriverRepository driverRepository,
            TractorRepository tractorRepository,
            SemiTrailerRepository semiTrailerRepository,
            ServiceRepository serviceRepository,
            TransportFacadeService transportFacadeService) {
        this.dashdocClient = dashdocClient;
        this.transportMapper = transportMapper;
        this.driverRepository = driverRepository;
        this.tractorRepository = tractorRepository;
        this.semiTrailerRepository = semiTrailerRepository;
        this.serviceRepository = serviceRepository;
        this.transportFacadeService = transportFacadeService;
    }

    public DashdocSyncResponse synchronize(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        long startedAt = System.nanoTime();
        log.info("Début de synchronisation Dashdoc pour la période {} – {}", startDate, endDate);

        List<JsonNode> transports = dashdocClient.getTransports();
        log.info("{} transport(s) récupéré(s) depuis Dashdoc", transports.size());
        List<Driver> drivers = driverRepository.findAll();
        List<Tractor> tractors = tractorRepository.findAll();
        List<SemiTrailer> semiTrailers = semiTrailerRepository.findAll();
        Map<String, Integer> skippedReasons = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (JsonNode transport : transports) {
            try {
                String externalId = transportMapper.externalId(transport);
                List<Service> existingServices = serviceRepository.findAllByExternalSourceAndExternalId(DASHDOC_SOURCE, externalId);
                if (existingServices.size() > 1) {
                    throw new DashdocTransportMappingException(
                        "Plusieurs services OptiRoute utilisent le même identifiant Dashdoc");
                }

                Service existing = existingServices.isEmpty() ? null : existingServices.getFirst();
                TransportRequest request = transportMapper.map(
                    transport,
                    startDate,
                    endDate,
                    drivers,
                    tractors,
                    semiTrailers,
                    existing == null || existing.getLoadingDurationMinutes() == null
                        ? DEFAULT_HANDLING_DURATION_MINUTES : existing.getLoadingDurationMinutes(),
                    existing == null || existing.getUnloadingDurationMinutes() == null
                        ? DEFAULT_HANDLING_DURATION_MINUTES : existing.getUnloadingDurationMinutes());

                if (existing == null) {
                    transportFacadeService.createTransport(request);
                    created++;
                    log.info("Transport Dashdoc {} importé (conducteur {}, tracteur {})",
                        externalId, request.driverId(), request.tractorId());
                } else {
                    transportFacadeService.updateTransport(existing.getId(), request);
                    updated++;
                    log.info("Transport Dashdoc {} mis à jour (service OptiRoute {})",
                        externalId, existing.getId());
                }
            } catch (DashdocTransportMappingException exception) {
                skipped++;
                skippedReasons.merge(exception.getMessage(), 1, Integer::sum);
                log.info("Transport Dashdoc {} ignoré : {}", safeExternalId(transport), exception.getMessage());
            } catch (DashdocTransportOutsideRangeException exception) {
                // Les transports hors de la période choisie ne sont pas des transports ignorés.
            } catch (TransportPlanningException | RoutingProviderException | DataAccessException exception) {
                String externalId = safeExternalId(transport);
                failures.add(externalId + " : " + exception.getMessage());
                log.warn("Échec de synchronisation du transport Dashdoc {} : {}", externalId, exception.getMessage());
            }
        }

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        log.info(
            "Synchronisation Dashdoc terminée en {} ms : {} créé(s), {} mis à jour, {} ignoré(s), {} échec(s)",
            elapsedMillis, created, updated, skipped, failures.size());

        return new DashdocSyncResponse(created, updated, skipped, skippedReasons, failures);
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La période sélectionnée est invalide.");
        }
        long selectedDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (selectedDays > 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La synchronisation est limitée à 7 jours.");
        }
    }

    private String safeExternalId(JsonNode transport) {
        try {
            return transportMapper.externalId(transport);
        } catch (DashdocTransportMappingException exception) {
            return "inconnu";
        }
    }
}
