package com.optiroute.backend.dto.response.route;

import java.math.BigDecimal;

import com.optiroute.backend.utils.MoneyUtils;

public class RouteCostDetailsDto {

    private BigDecimal fuelCost = MoneyUtils.ZERO;
    private BigDecimal tollCost = MoneyUtils.ZERO;

    private BigDecimal totalCost = MoneyUtils.ZERO;

    public BigDecimal getFuelCost() {
        return fuelCost;
    }

    public void setFuelCost(BigDecimal fuelCost) {
        this.fuelCost = MoneyUtils.round(fuelCost);
    }

    public BigDecimal getTollCost() {
        return tollCost;
    }

    public void setTollCost(BigDecimal tollCost) {
        this.tollCost = MoneyUtils.round(tollCost);
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = MoneyUtils.round(totalCost);
    }
}
