package com.optiroute.backend.service.route;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.optiroute.backend.dto.response.route.RouteCostDetailsDto;
import com.optiroute.backend.utils.MoneyUtils;

@Service
public class RouteCostService {

    public RouteCostDetailsDto calculateCosts(double distanceKm, double fuelConsumptionPer100Km, double fuelPrice, double tollCost) {

        double fuelUsed = (distanceKm / 100.0) * fuelConsumptionPer100Km;
        BigDecimal fuelCost = MoneyUtils.multiply(MoneyUtils.of(fuelPrice), fuelUsed);

        RouteCostDetailsDto dto = new RouteCostDetailsDto();
        dto.setFuelCost(fuelCost);
        dto.setTollCost(MoneyUtils.of(tollCost));
        dto.setTotalCost(dto.getFuelCost().add(dto.getTollCost()));

        return dto;
    }
}
