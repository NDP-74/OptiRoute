package com.optiroute.backend.service.cost;

import org.springframework.stereotype.Service;

import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.entity.cost.CostCondition;
import com.optiroute.backend.repository.cost.CostParameterRepository;
import com.optiroute.backend.type.cost.CostParameterAssignmentType;
import com.optiroute.backend.type.cost.CostParameterCategoryType;
import com.optiroute.backend.type.cost.CostParameterUnitType;

import com.optiroute.backend.dto.response.cost.AppliedCostResponse;
import com.optiroute.backend.dto.request.cost.CostCalculationContext;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CostParameterEngine {

    private final CostParameterRepository costParameterRepository;
    private final CostConditionService costConditionService;
    private final WorkingDaysService workingDaysService;

    public CostParameterEngine(CostParameterRepository costParameterRepository, CostConditionService costConditionService, WorkingDaysService workingDaysService) {
        this.costParameterRepository = costParameterRepository;
        this.costConditionService = costConditionService;
        this.workingDaysService = workingDaysService;
    }

    public List<AppliedCostResponse> calculateCosts(CostParameterCategoryType category, CostCalculationContext context) {
        return calculateCosts(category,context,List.of(context));
    }

    public List<AppliedCostResponse> calculateCosts(CostParameterCategoryType category, CostCalculationContext context, List<CostCalculationContext> dailyContexts) {
        return calculateCosts(category,context,dailyContexts,costParameterRepository.findByCategory(category),null);
    }

    public CostParameterSnapshot loadSnapshot() {
        List<CostParameter> parameters = costParameterRepository
            .findByCategoryIn(List.of(CostParameterCategoryType.VEHICLE,CostParameterCategoryType.DRIVER,CostParameterCategoryType.STRUCTURE));
        List<Long> parameterIds = parameters.stream().map(CostParameter::getId).toList();
        Map<Long, List<CostCondition>> conditionsByParameterId = parameterIds.isEmpty() ? Map.of() : costConditionService.findByCostParameterIds(parameterIds);
        Map<CostParameterCategoryType, List<CostParameter>> parametersByCategory = parameters.stream().collect(Collectors.groupingBy(CostParameter::getCategory));
        return new CostParameterSnapshot(parametersByCategory, conditionsByParameterId);
    }

    public List<AppliedCostResponse> calculateCosts(CostParameterCategoryType category, CostCalculationContext context, List<CostCalculationContext> dailyContexts,
        CostParameterSnapshot snapshot) {
        return calculateCosts(category,context,dailyContexts,snapshot.parametersByCategory().getOrDefault(category,List.of()),snapshot.conditionsByParameterId());
    }

    private List<AppliedCostResponse> calculateCosts(CostParameterCategoryType category, CostCalculationContext context, List<CostCalculationContext> dailyContexts,
        List<CostParameter> parameters, Map<Long, List<CostCondition>> conditionsByParameterId) {

        List<AppliedCostResponse> costs = new ArrayList<>();

        for (CostParameter parameter : parameters) {
            if (parameter.isActive() && parameter.getAssignmentType() != CostParameterAssignmentType.MANUAL && parameter.getUnit() == CostParameterUnitType.EUR_PER_NIGHT) {
                var triggeringDays = dailyContexts.stream().filter(c -> c.date().equals(context.date()) || c.date().equals(context.date().minusDays(1)))
                    .collect(Collectors.groupingBy(CostCalculationContext::date));
                double amount = 0;
                boolean applicable = false;
                for (List<CostCalculationContext> day : triggeringDays.values()) {
                    if (day.stream().anyMatch(c -> isApplicable(parameter,c,conditionsByParameterId))) {
                        amount += calculateAmount(parameter,day.getFirst());
                        applicable = true;
                    }
                }
                if (applicable) {
                    costs.add(new AppliedCostResponse(parameter.getLabel(), amount));
                }
                continue;
            }
            if (!parameter.isActive() || parameter.getAssignmentType() == CostParameterAssignmentType.MANUAL
                || !isApplicable(parameter,context,dailyContexts,conditionsByParameterId)) {
                continue;
            }

            double amount = calculateAmount(parameter,context);
            costs.add(new AppliedCostResponse(parameter.getLabel(), amount));
        }

        return costs;
    }

    // Coût d'un paramètre MANUAL rattaché à un événement : pas de conditions,
    // uniquement sur le trajet de l'événement
    public AppliedCostResponse calculateManualCost(CostParameter parameter, CostCalculationContext context) {
        if (!parameter.isActive() || parameter.getAssignmentType() != CostParameterAssignmentType.MANUAL) {
            return null;
        }
        return new AppliedCostResponse(parameter.getLabel(), calculateAmount(parameter,context));
    }

    private boolean isApplicable(CostParameter parameter, CostCalculationContext context, Map<Long, List<CostCondition>> conditionsByParameterId) {
        if (conditionsByParameterId == null) {
            return costConditionService.isApplicable(parameter,context);
        }
        return costConditionService.isApplicable(parameter,context,conditionsByParameterId.getOrDefault(parameter.getId(),List.of()));
    }

    private boolean isApplicable(CostParameter parameter, CostCalculationContext context, List<CostCalculationContext> dailyContexts,
        Map<Long, List<CostCondition>> conditionsByParameterId) {
        if (parameter.getUnit() != CostParameterUnitType.EUR_PER_DAY) {
            return isApplicable(parameter,context,conditionsByParameterId);
        }

        return dailyContexts.stream().filter(c -> c.date().equals(context.date())).anyMatch(dailyContext -> isApplicable(parameter,dailyContext,conditionsByParameterId));
    }

    private double calculateAmount(CostParameter parameter, CostCalculationContext context) {
        return switch (parameter.getUnit()) {

            case EUR_PER_KM -> context.distanceKm() * parameter.getValue().doubleValue();

            case EUR_PER_TRIP -> parameter.getValue().doubleValue();

            case EUR_PER_HOUR -> context.durationHours() * parameter.getValue().doubleValue();

            case EUR_PER_DAY -> calculateDailyCost(parameter,context);

            case EUR_PER_NIGHT -> {
                if (context.nightTransportCount() <= 0) {
                    throw new IllegalStateException("Night costs require a positive two-day transport count");
                }
                yield parameter.getValue().doubleValue() / context.nightTransportCount();
            }

            case EUR_PER_MONTH -> calculateMonthlyCost(parameter,context);

            case EUR_PER_YEAR -> calculateYearlyCost(parameter,context);
        };
    }

    private double calculateDailyCost(CostParameter parameter, CostCalculationContext context) {
        if (context.dailyTransportCount() <= 0) {
            return parameter.getValue().doubleValue();
        }

        return parameter.getValue().doubleValue() / context.dailyTransportCount();
    }

    private double calculateMonthlyCost(CostParameter parameter, CostCalculationContext context) {
        int workingDays = workingDaysService.getWorkingDaysInMonth(YearMonth.from(context.date()));

        if (workingDays <= 0 || context.dailyVehicleDistanceKm() <= 0) {
            return 0;
        }

        double dailyCost = parameter.getValue().doubleValue() / workingDays;
        return dailyCost * (context.distanceKm() / context.dailyVehicleDistanceKm());
    }

    private double calculateYearlyCost(CostParameter parameter, CostCalculationContext context) {
        int workingDays = workingDaysService.getWorkingDaysInYear(context.date().getYear());

        if (workingDays <= 0 || context.dailyTransportCount() <= 0) {
            return 0;
        }

        double dailyCost = parameter.getValue().doubleValue() / workingDays;
        return dailyCost / context.dailyTransportCount();
    }
}
