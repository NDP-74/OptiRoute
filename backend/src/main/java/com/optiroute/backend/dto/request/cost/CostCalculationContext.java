package com.optiroute.backend.dto.request.cost;

import java.time.LocalDate;
import java.time.LocalTime;

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

    int nightTransportCount) {

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