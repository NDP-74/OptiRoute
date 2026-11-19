package com.optiroute.backend.dto.request.cost;

import com.optiroute.backend.type.cost.CostParameterAssignmentType;
import com.optiroute.backend.type.cost.CostParameterCategoryType;
import com.optiroute.backend.type.cost.CostParameterUnitType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code active} absent : le paramètre est actif (valeur par défaut de l'entité).
 */
public record UpdateCostParameterRequest(@NotNull CostParameterCategoryType category, @NotBlank String label,
    @NotNull @PositiveOrZero BigDecimal value, @NotNull CostParameterUnitType unit, Boolean active,
    CostParameterAssignmentType assignmentType, List<@Valid @NotNull CostConditionRequest> conditions) {

    public boolean isActive() {
        return active == null || active;
    }
}
