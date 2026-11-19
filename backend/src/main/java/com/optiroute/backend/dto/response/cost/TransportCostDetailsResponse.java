package com.optiroute.backend.dto.response.cost;

import java.math.BigDecimal;

import com.optiroute.backend.utils.MoneyUtils;

public record TransportCostDetailsResponse(CostCategoryResponse vehicle, CostCategoryResponse driver, CostCategoryResponse structure, BigDecimal totalCost) {

    public TransportCostDetailsResponse {
        totalCost = MoneyUtils.round(totalCost);
    }

    public static TransportCostDetailsResponse of(CostCategoryResponse vehicle, CostCategoryResponse driver, CostCategoryResponse structure) {
        return new TransportCostDetailsResponse(vehicle, driver, structure,
            vehicle.totalCost().add(driver.totalCost()).add(structure.totalCost()));
    }
}
