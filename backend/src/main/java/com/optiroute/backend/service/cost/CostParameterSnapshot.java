package com.optiroute.backend.service.cost;

import java.util.List;
import java.util.Map;

import com.optiroute.backend.entity.cost.CostCondition;
import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.type.cost.CostParameterCategoryType;

public record CostParameterSnapshot(
    Map<CostParameterCategoryType, List<CostParameter>> parametersByCategory,
    Map<Long, List<CostCondition>> conditionsByParameterId
) {
}
