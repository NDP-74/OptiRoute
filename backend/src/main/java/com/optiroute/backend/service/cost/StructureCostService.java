package com.optiroute.backend.service.cost;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.optiroute.backend.dto.response.cost.AppliedCostResponse;
import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.type.cost.CostParameterCategoryType;

import com.optiroute.backend.dto.request.cost.CostCalculationContext;

@Service
@RequiredArgsConstructor
public class StructureCostService {

    private final CostParameterEngine costParameterEngine;

    public CostCategoryResponse calculateCosts(CostCalculationContext context) {
        return calculateCosts(context,List.of(context));
    }

    public CostCategoryResponse calculateCosts(CostCalculationContext context, List<CostCalculationContext> dailyContexts) {
        return calculateCosts(context, dailyContexts, null);
    }

    public CostCategoryResponse calculateCosts(
        CostCalculationContext context,
        List<CostCalculationContext> dailyContexts,
        CostParameterSnapshot snapshot
    ) {

        // CostParameters STRUCTURE
        List<AppliedCostResponse> costs = snapshot == null
            ? costParameterEngine.calculateCosts(CostParameterCategoryType.STRUCTURE,context,dailyContexts)
            : costParameterEngine.calculateCosts(CostParameterCategoryType.STRUCTURE,context,dailyContexts,snapshot);

        return CostCategoryResponse.of(costs);
    }
}