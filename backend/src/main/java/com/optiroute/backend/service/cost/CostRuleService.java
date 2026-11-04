package com.optiroute.backend.service.cost;

import org.springframework.stereotype.Service;
import com.optiroute.backend.entity.cost.CostRule;
import com.optiroute.backend.entity.cost.CostCondition;
import com.optiroute.backend.entity.cost.CostParameter;

import com.optiroute.backend.repository.cost.CostRuleRepository;
import com.optiroute.backend.repository.cost.CostConditionRepository;

import com.optiroute.backend.type.cost.CostRuleLogicalOperatorType;
import com.optiroute.backend.dto.request.cost.CostCalculationContext;

import java.util.List;
import java.util.Objects;
import java.time.LocalTime;

@Service
public class CostRuleService {

    private final CostRuleRepository costRuleRepository;
    private final CostConditionRepository costConditionRepository;

    public CostRuleService(CostRuleRepository costRuleRepository, CostConditionRepository costConditionRepository) {
        this.costRuleRepository = costRuleRepository;
        this.costConditionRepository = costConditionRepository;
    }

    public boolean isApplicable(CostParameter parameter, CostCalculationContext context) {
        List<CostRule> rules = costRuleRepository.findByCostParameterId(parameter.getId());

        // Aucune règle = toujours applicable
        if (rules.isEmpty()) {
            return true;
        }

        // Une seule règle vraie suffit
        return rules.stream().anyMatch(rule -> evaluateRule(rule,context));
    }

    private boolean evaluateRule(CostRule rule, CostCalculationContext context) {

        List<CostCondition> conditions = costConditionRepository.findByCostRuleId(rule.getId());
        if (conditions.isEmpty()) {
            return false;
        }

        if (rule.getLogicalOperator() == CostRuleLogicalOperatorType.AND) {
            return conditions.stream().allMatch(condition -> evaluateCondition(condition,context));
        }

        return conditions.stream().anyMatch(condition -> evaluateCondition(condition,context));
    }

    private boolean evaluateCondition(CostCondition condition, CostCalculationContext context) {
        return switch (condition.getField()) {

            case DISTANCE -> evaluateNumeric(context.distanceKm(),condition);

            case DURATION -> evaluateNumeric(context.durationHours(),condition);

            case DEPARTURE_TIME -> evaluateTime(context.departureTime(),condition);

            case ARRIVAL_TIME -> evaluateTime(context.arrivalTime(),condition);

            case EMPTY_TRIP -> evaluateBoolean(context.emptyTrip(),condition);

            case LOADED_TRIP -> evaluateBoolean(!context.emptyTrip(),condition);

            case DRIVER_DAY_START_TIME -> evaluateTime(context.driverDayStartTime(),condition);

            case DRIVER_DAY_END_TIME -> evaluateTime(context.driverDayEndTime(),condition);

            case VEHICLE_TYPE -> evaluateVehicleTypeId(context.trailerTypeId(),condition);
        };
    }

    private boolean evaluateNumeric(double actualValue, CostCondition condition) {
        double expectedValue = Double.parseDouble(condition.getValue());

        return switch (condition.getOperator()) {

            case EQUALS -> actualValue == expectedValue;

            case NOT_EQUALS -> actualValue != expectedValue;

            case LESS_THAN -> actualValue < expectedValue;

            case GREATER_THAN -> actualValue > expectedValue;

            default -> throw new IllegalArgumentException("Invalid operator for numeric condition: " + condition.getOperator());
        };
    }

    private boolean evaluateTime(LocalTime actualValue, CostCondition condition) {
        LocalTime expectedValue = LocalTime.parse(condition.getValue());

        return switch (condition.getOperator()) {

            case EQUALS -> actualValue.equals(expectedValue);

            case NOT_EQUALS -> !actualValue.equals(expectedValue);

            case BEFORE -> actualValue.isBefore(expectedValue);

            case AFTER -> actualValue.isAfter(expectedValue);

            default -> throw new IllegalArgumentException("Invalid operator for time condition: " + condition.getOperator());
        };
    }

    private boolean evaluateBoolean(boolean actualValue, CostCondition condition) {
        boolean expectedValue = Boolean.parseBoolean(condition.getValue());

        return switch (condition.getOperator()) {

            case EQUALS -> actualValue == expectedValue;

            case NOT_EQUALS -> actualValue != expectedValue;

            default -> throw new IllegalArgumentException("Invalid operator for boolean condition: " + condition.getOperator());
        };
    }

    private boolean evaluateVehicleTypeId(Long actualValue, CostCondition condition) {
        Long expectedValue = parseLongOrNull(condition.getValue());

        return switch (condition.getOperator()) {

            case EQUALS -> Objects.equals(actualValue,expectedValue);

            case NOT_EQUALS -> !Objects.equals(actualValue,expectedValue);

            default -> throw new IllegalArgumentException("Invalid operator for vehicle type condition: " + condition.getOperator());
        };
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}