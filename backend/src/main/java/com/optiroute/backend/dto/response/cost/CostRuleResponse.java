package com.optiroute.backend.dto.response.cost;

import com.optiroute.backend.type.cost.CostRuleLogicalOperatorType;

import java.util.List;

public record CostRuleResponse(Long id, CostRuleLogicalOperatorType logicalOperator, List<CostConditionResponse> conditions) {
}
