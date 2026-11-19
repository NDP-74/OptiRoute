package com.optiroute.backend.dto.request.cost;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Contexte de calcul d'un trajet.
 * <p>
 * Les heures sont exprimées en Europe/Paris. {@code arrivalDayOffset} et {@code driverDayEndDayOffset} indiquent
 * le nombre de jours écoulés depuis {@code date} (jour de départ) : une arrivée à 01:30 le lendemain a l'heure
 * 01:30 et un décalage de 1, afin que les conditions horaires la comparent comme 25:30.
 */
public record CostCalculationContext(LocalDate date,

    double distanceKm, double dailyVehicleDistanceKm,

    double durationHours,

    int dailyTransportCount,

    boolean emptyTrip,

    LocalTime departureTime,

    LocalTime arrivalTime,

    LocalTime driverDayStartTime,

    LocalTime driverDayEndTime,

    Long trailerTypeId,

    double dailyDriverDurationHours,

    double driverWorkDurationHours,

    Boolean driverEndsDayAtParking,

    int nightTransportCount,

    int arrivalDayOffset,

    int driverDayEndDayOffset) {

    public CostCalculationContext(LocalDate date, double distanceKm, double dailyVehicleDistanceKm, double durationHours, int dailyTransportCount, boolean emptyTrip,
        LocalTime departureTime, LocalTime arrivalTime, LocalTime driverDayStartTime, LocalTime driverDayEndTime, Long trailerTypeId, double dailyDriverDurationHours,
        double driverWorkDurationHours, Boolean driverEndsDayAtParking, int nightTransportCount) {
        this(date, distanceKm, dailyVehicleDistanceKm, durationHours, dailyTransportCount, emptyTrip, departureTime, arrivalTime, driverDayStartTime, driverDayEndTime,
            trailerTypeId, dailyDriverDurationHours, driverWorkDurationHours, driverEndsDayAtParking, nightTransportCount, 0, 0);
    }

    public CostCalculationContext(LocalDate date, double distanceKm, double dailyVehicleDistanceKm, double durationHours, int dailyTransportCount, boolean emptyTrip,
        LocalTime departureTime, LocalTime arrivalTime, LocalTime driverDayStartTime, LocalTime driverDayEndTime, Long trailerTypeId, double dailyDriverDurationHours,
        double driverWorkDurationHours, Boolean driverEndsDayAtParking) {
        this(date, distanceKm, dailyVehicleDistanceKm, durationHours, dailyTransportCount, emptyTrip, departureTime, arrivalTime, driverDayStartTime, driverDayEndTime,
            trailerTypeId, dailyDriverDurationHours, driverWorkDurationHours, driverEndsDayAtParking, dailyTransportCount);
    }

    public CostCalculationContext(LocalDate date, double distanceKm, double dailyVehicleDistanceKm, double durationHours, int dailyTransportCount, boolean emptyTrip,
        LocalTime departureTime, LocalTime arrivalTime, LocalTime driverDayStartTime, LocalTime driverDayEndTime, Long trailerTypeId, double dailyDriverDurationHours,
        double driverWorkDurationHours) {
        this(date, distanceKm, dailyVehicleDistanceKm, durationHours, dailyTransportCount, emptyTrip, departureTime, arrivalTime, driverDayStartTime, driverDayEndTime,
            trailerTypeId, dailyDriverDurationHours, driverWorkDurationHours, null);
    }

    public CostCalculationContext(LocalDate date, double distanceKm, double dailyVehicleDistanceKm, double durationHours, int dailyTransportCount, boolean emptyTrip,
        LocalTime departureTime, LocalTime arrivalTime, LocalTime driverDayStartTime, LocalTime driverDayEndTime, Long trailerTypeId, double dailyDriverDurationHours) {
        this(date, distanceKm, dailyVehicleDistanceKm, durationHours, dailyTransportCount, emptyTrip, departureTime, arrivalTime, driverDayStartTime, driverDayEndTime,
            trailerTypeId, dailyDriverDurationHours, durationHours);
    }
}
