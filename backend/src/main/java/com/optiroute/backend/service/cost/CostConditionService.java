package com.optiroute.backend.service.cost;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.optiroute.backend.entity.cost.CostCondition;
import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.repository.cost.CostConditionRepository;
import com.optiroute.backend.type.cost.CostConditionFieldType;
import com.optiroute.backend.type.cost.CostConditionOperatorType;
import com.optiroute.backend.type.cost.CostConditionSourceType;
import com.optiroute.backend.type.cost.CostParameterAssignmentType;
import com.optiroute.backend.dto.request.cost.CostCalculationContext;
import com.optiroute.backend.dto.request.cost.CostConditionRequest;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CostConditionService {

    private final CostConditionRepository costConditionRepository;

    public CostConditionService(CostConditionRepository costConditionRepository) {
        this.costConditionRepository = costConditionRepository;
    }

    public boolean isApplicable(CostParameter parameter, CostCalculationContext context) {
        if (parameter.getAssignmentType() == CostParameterAssignmentType.ALWAYS) {
            return true;
        }
        if (parameter.getAssignmentType() == CostParameterAssignmentType.MANUAL) {
            return false;
        }
        return isApplicable(parameter, context, costConditionRepository.findByCostParameterId(parameter.getId()));
    }

    public Map<Long, List<CostCondition>> findByCostParameterIds(List<Long> parameterIds) {
        return costConditionRepository.findByCostParameterIdIn(parameterIds).stream()
            .collect(Collectors.groupingBy(condition -> condition.getCostParameter().getId()));
    }

    public boolean isApplicable(CostParameter parameter, CostCalculationContext context, List<CostCondition> conditions) {
        if (parameter.getAssignmentType() == CostParameterAssignmentType.ALWAYS) {
            return true;
        }
        if (parameter.getAssignmentType() == CostParameterAssignmentType.MANUAL) {
            return false;
        }
        if (conditions.isEmpty()) {
            return true;
        }

        if (parameter.getAssignmentType() == CostParameterAssignmentType.ALL_CONDITIONS) {
            return conditions.stream().allMatch(condition -> evaluateCondition(condition, context));
        }

        if (parameter.getAssignmentType() == CostParameterAssignmentType.ANY_CONDITION) {
            return conditions.stream().anyMatch(condition -> evaluateCondition(condition, context));
        }

        return false;
    }

    /**
     * Vérifie la cohérence source / champ / opérateur / valeur d'une condition avant enregistrement.
     *
     * @throws ResponseStatusException 400 si la condition ne pourrait pas être évaluée
     */
    public static void validate(CostConditionRequest request) {
        CostConditionFieldType field = request.field();
        if (request.source() == null || field == null || request.operator() == null || request.value() == null || request.value().isBlank()) {
            throw badCondition("source, champ, opérateur et valeur sont obligatoires.");
        }
        if (sourceOf(field) != request.source()) {
            throw badCondition("le champ " + field + " n'appartient pas à la source " + request.source() + ".");
        }
        if (!allowedOperators(field).contains(request.operator())) {
            throw badCondition("l'opérateur " + request.operator() + " n'est pas autorisé pour le champ " + field + ".");
        }
        String value = request.value().trim();
        switch (field) {
            case DISTANCE, DURATION -> {
                try {
                    if (!Double.isFinite(Double.parseDouble(value))) {
                        throw badCondition("la valeur numérique est invalide : " + value + ".");
                    }
                } catch (NumberFormatException exception) {
                    throw badCondition("la valeur numérique est invalide : " + value + ".");
                }
            }
            case DEPARTURE_TIME, ARRIVAL_TIME, DRIVER_DAY_START_TIME, DRIVER_DAY_END_TIME -> {
                try {
                    LocalTime.parse(value);
                } catch (DateTimeParseException exception) {
                    throw badCondition("l'heure doit être au format HH:mm : " + value + ".");
                }
            }
            case EMPTY_TRIP, LOADED_TRIP, DRIVER_ENDS_DAY_AT_PARKING -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw badCondition("la valeur doit être true ou false : " + value + ".");
                }
            }
            case VEHICLE_TYPE -> {
                if (parseLongOrNull(value) == null) {
                    throw badCondition("le type de semi-remorque est invalide : " + value + ".");
                }
            }
        }
    }

    private static CostConditionSourceType sourceOf(CostConditionFieldType field) {
        return switch (field) {
            case DISTANCE, DURATION, DEPARTURE_TIME, ARRIVAL_TIME, EMPTY_TRIP, LOADED_TRIP -> CostConditionSourceType.TRIP;
            case VEHICLE_TYPE -> CostConditionSourceType.VEHICLE;
            case DRIVER_DAY_START_TIME, DRIVER_DAY_END_TIME, DRIVER_ENDS_DAY_AT_PARKING -> CostConditionSourceType.DRIVER;
        };
    }

    private static Set<CostConditionOperatorType> allowedOperators(CostConditionFieldType field) {
        return switch (field) {
            case DISTANCE, DURATION -> EnumSet.of(CostConditionOperatorType.EQUALS, CostConditionOperatorType.NOT_EQUALS,
                CostConditionOperatorType.LESS_THAN, CostConditionOperatorType.GREATER_THAN);
            case DEPARTURE_TIME, ARRIVAL_TIME, DRIVER_DAY_START_TIME, DRIVER_DAY_END_TIME -> EnumSet.of(CostConditionOperatorType.EQUALS,
                CostConditionOperatorType.NOT_EQUALS, CostConditionOperatorType.BEFORE, CostConditionOperatorType.AFTER);
            case EMPTY_TRIP, LOADED_TRIP, DRIVER_ENDS_DAY_AT_PARKING, VEHICLE_TYPE -> EnumSet.of(CostConditionOperatorType.EQUALS,
                CostConditionOperatorType.NOT_EQUALS);
        };
    }

    private static ResponseStatusException badCondition(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Condition invalide : " + reason);
    }

    // Une condition invalide déjà en base est considérée non satisfaite au lieu de faire échouer tout le calcul
    private boolean evaluateCondition(CostCondition condition, CostCalculationContext context) {
        try {
            return evaluateValidCondition(condition, context);
        } catch (RuntimeException exception) {
            log.warn("Condition de coût {} ignorée (champ={}, opérateur={}, valeur={}) : {}", condition.getId(), condition.getField(),
                condition.getOperator(), condition.getValue(), exception.getMessage());
            return false;
        }
    }

    private boolean evaluateValidCondition(CostCondition condition, CostCalculationContext context) {
        return switch (condition.getField()) {
            case DISTANCE -> evaluateNumeric(context.distanceKm(), condition);
            case DURATION -> evaluateNumeric(context.durationHours(), condition);
            case DEPARTURE_TIME -> evaluateTime(context.departureTime(), 0, condition);
            case ARRIVAL_TIME -> evaluateTime(context.arrivalTime(), context.arrivalDayOffset(), condition);
            case EMPTY_TRIP -> evaluateBoolean(context.emptyTrip(), condition);
            case LOADED_TRIP -> evaluateBoolean(!context.emptyTrip(), condition);
            case DRIVER_DAY_START_TIME -> evaluateTime(context.driverDayStartTime(), 0, condition);
            case DRIVER_DAY_END_TIME -> evaluateTime(context.driverDayEndTime(), context.driverDayEndDayOffset(), condition);
            case DRIVER_ENDS_DAY_AT_PARKING -> context.driverEndsDayAtParking() != null
                && evaluateBoolean(context.driverEndsDayAtParking(), condition);
            case VEHICLE_TYPE -> evaluateVehicleTypeId(context.trailerTypeId(), condition);
        };
    }

    private boolean evaluateNumeric(double actualValue, CostCondition condition) {
        double expectedValue = Double.parseDouble(condition.getValue().trim());
        return switch (condition.getOperator()) {
            case EQUALS -> actualValue == expectedValue;
            case NOT_EQUALS -> actualValue != expectedValue;
            case LESS_THAN -> actualValue < expectedValue;
            case GREATER_THAN -> actualValue > expectedValue;
            default -> throw new IllegalArgumentException("Invalid operator for numeric condition: " + condition.getOperator());
        };
    }

    /**
     * Compare une heure au seuil de la condition sur la journée de départ.
     * {@code dayOffset} jours sont ajoutés à l'heure réelle : une arrivée à 01:30 le lendemain vaut 25:30,
     * elle est donc après 20:00 et n'est pas égale à 01:30.
     */
    private boolean evaluateTime(LocalTime actualValue, int dayOffset, CostCondition condition) {
        if (actualValue == null) {
            return false;
        }
        long actualSeconds = dayOffset * 86_400L + actualValue.toSecondOfDay();
        long expectedSeconds = LocalTime.parse(condition.getValue().trim()).toSecondOfDay();
        return switch (condition.getOperator()) {
            case EQUALS -> actualSeconds == expectedSeconds;
            case NOT_EQUALS -> actualSeconds != expectedSeconds;
            case BEFORE -> actualSeconds < expectedSeconds;
            case AFTER -> actualSeconds > expectedSeconds;
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
            case EQUALS -> Objects.equals(actualValue, expectedValue);
            case NOT_EQUALS -> !Objects.equals(actualValue, expectedValue);
            default -> throw new IllegalArgumentException("Invalid operator for vehicle type condition: " + condition.getOperator());
        };
    }

    private static Long parseLongOrNull(String value) {
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
