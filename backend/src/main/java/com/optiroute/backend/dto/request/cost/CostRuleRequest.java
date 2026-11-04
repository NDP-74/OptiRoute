package com.optiroute.backend.dto.request.cost;

import com.optiroute.backend.type.cost.CostRuleLogicalOperatorType;

import java.util.List;

public record CostRuleRequest(CostRuleLogicalOperatorType logicalOperator, List<CostConditionRequest> conditions) {
}
