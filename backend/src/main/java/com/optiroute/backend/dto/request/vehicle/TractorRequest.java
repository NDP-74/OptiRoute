package com.optiroute.backend.dto.request.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.optiroute.backend.type.FuelType;

public record TractorRequest(String externalId, String externalSource,

        String registration,

        String brand, String model,

        Integer maxSpeed,

        FuelType fuelType, BigDecimal averageConsumption, BigDecimal averageConsumptionEmpty,

        Integer emptyWeightKg, Integer grossCombinationWeightKg,

        Integer heightCm, Integer widthCm, Integer lengthCm, Integer axleCount,

        BigDecimal purchaseCost, LocalDate depreciationStartDate, LocalDate depreciationEndDate,

        BigDecimal maintenanceCostPerKm,

        OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
