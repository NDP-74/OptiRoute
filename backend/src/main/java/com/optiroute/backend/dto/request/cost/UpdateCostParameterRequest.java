package com.optiroute.backend.dto.request.cost;

import com.optiroute.backend.type.cost.CostParameterAssignmentType;
import com.optiroute.backend.type.cost.CostParameterCategoryType;
import com.optiroute.backend.type.cost.CostParameterUnitType;

import java.math.BigDecimal;

public record UpdateCostParameterRequest(CostParameterCategoryType category, String label, BigDecimal value, CostParameterUnitType unit, boolean active, CostParameterAssignmentType assignmentType, CostRuleRequest rule) {
}