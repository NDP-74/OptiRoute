package com.optiroute.backend.dto.response.cost;

import com.optiroute.backend.type.cost.CostParameterAssignmentType;
import com.optiroute.backend.type.cost.CostParameterCategoryType;
import com.optiroute.backend.type.cost.CostParameterUnitType;

import java.math.BigDecimal;
import java.util.List;

public record CostParameterResponse(Long id, CostParameterCategoryType category, String label, BigDecimal value, CostParameterUnitType unit, boolean active,
    CostParameterAssignmentType assignmentType, List<CostConditionResponse> conditions) {
}