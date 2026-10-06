package com.optiroute.backend.dto.response.cost;

import com.optiroute.backend.type.cost.CostParameterAssignmentType;
import com.optiroute.backend.type.cost.CostParameterCategoryType;
import com.optiroute.backend.type.cost.CostParameterUnitType;

import java.math.BigDecimal;

public record CostParameterLightResponse(Long id, String label, BigDecimal value, CostParameterUnitType unit, CostParameterCategoryType category, boolean active, CostParameterAssignmentType assignmentType) {
}