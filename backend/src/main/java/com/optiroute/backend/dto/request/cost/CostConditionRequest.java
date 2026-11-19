package com.optiroute.backend.dto.request.cost;

import com.optiroute.backend.type.cost.CostConditionFieldType;
import com.optiroute.backend.type.cost.CostConditionOperatorType;
import com.optiroute.backend.type.cost.CostConditionSourceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CostConditionRequest(@NotNull CostConditionSourceType source, @NotNull CostConditionFieldType field,
    @NotNull CostConditionOperatorType operator, @NotBlank @Size(max = 100) String value) {
}
