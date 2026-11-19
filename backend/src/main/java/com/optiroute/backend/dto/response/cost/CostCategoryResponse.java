package com.optiroute.backend.dto.response.cost;

import java.math.BigDecimal;
import java.util.List;

import com.optiroute.backend.utils.MoneyUtils;

public record CostCategoryResponse(List<AppliedCostResponse> costs, BigDecimal totalCost) {

    public CostCategoryResponse {
        totalCost = MoneyUtils.round(totalCost);
    }

    // Total = somme des montants déjà arrondis, pour que l'affichage soit cohérent ligne à ligne
    public static CostCategoryResponse of(List<AppliedCostResponse> costs) {
        return new CostCategoryResponse(costs, MoneyUtils.sum(costs, AppliedCostResponse::amount));
    }

    public static CostCategoryResponse empty() {
        return new CostCategoryResponse(List.of(), MoneyUtils.ZERO);
    }
}
