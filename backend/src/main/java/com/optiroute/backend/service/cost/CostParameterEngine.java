package com.optiroute.backend.service.cost;

import org.springframework.stereotype.Service;

import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.repository.cost.CostParameterRepository;
import com.optiroute.backend.type.cost.CostParameterAssignmentType;
import com.optiroute.backend.type.cost.CostParameterCategoryType;

import com.optiroute.backend.dto.response.cost.AppliedCostResponse;
import com.optiroute.backend.dto.request.cost.CostCalculationContext;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class CostParameterEngine {

    private final CostParameterRepository costParameterRepository;
    private final CostRuleService costRuleService;
    private final WorkingDaysService workingDaysService;

    public CostParameterEngine(CostParameterRepository costParameterRepository, CostRuleService costRuleService, WorkingDaysService workingDaysService) {
        this.costParameterRepository = costParameterRepository;
        this.costRuleService = costRuleService;
        this.workingDaysService = workingDaysService;
    }

    public List<AppliedCostResponse> calculateCosts(CostParameterCategoryType category, CostCalculationContext context) {
        return calculateCosts(category,context,List.of(context));
    }

    public List<AppliedCostResponse> calculateCosts(CostParameterCategoryType category, CostCalculationContext context, List<CostCalculationContext> dailyContexts) {

        List<AppliedCostResponse> costs = new ArrayList<>();

        for (CostParameter parameter : costParameterRepository.findByCategory(category)) {
            if (!parameter.isActive() || parameter.getAssignmentType() == CostParameterAssignmentType.MANUAL || !isApplicable(parameter,context,dailyContexts)) {
                continue;
            }

            double amount = calculateAmount(parameter,context);
            costs.add(new AppliedCostResponse(parameter.getLabel(), amount));
        }

        return costs;
    }

    // Coût d'un paramètre MANUAL rattaché à un événement : pas de conditions, uniquement sur le trajet de l'événement
    public AppliedCostResponse calculateManualCost(CostParameter parameter, CostCalculationContext context) {
        if (!parameter.isActive() || parameter.getAssignmentType() != CostParameterAssignmentType.MANUAL) {
            return null;
        }
        return new AppliedCostResponse(parameter.getLabel(),calculateAmount(parameter,context));
    }

    private boolean isApplicable(CostParameter parameter, CostCalculationContext context, List<CostCalculationContext> dailyContexts) {
        if (parameter.getUnit() != com.optiroute.backend.type.cost.CostParameterUnitType.EUR_PER_DAY) {
            return costRuleService.isApplicable(parameter,context);
        }

        return dailyContexts.stream().anyMatch(dailyContext -> costRuleService.isApplicable(parameter,dailyContext));
    }

    private double calculateAmount(CostParameter parameter, CostCalculationContext context) {
        return switch (parameter.getUnit()) {

            case EUR_PER_KM -> context.distanceKm() * parameter.getValue().doubleValue();

            case EUR_PER_TRIP -> parameter.getValue().doubleValue();

            case EUR_PER_HOUR -> context.durationHours() * parameter.getValue().doubleValue();

            case EUR_PER_DAY -> calculateDailyCost(parameter,context);

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
