package com.optiroute.backend.service.cost;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.optiroute.backend.dto.request.cost.CostCalculationContext;
import com.optiroute.backend.dto.response.cost.AppliedCostResponse;
import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.entity.driver.Driver;
import com.optiroute.backend.type.cost.CostParameterCategoryType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DriverCostService {
    private static final String SALARY_LABEL = "Salaire";

    private final WorkingDaysService workingDaysService;
    private final CostParameterEngine costParameterEngine;

    public CostCategoryResponse calculateCosts(Driver driver, CostCalculationContext context) {
        return calculateCosts(driver,context,List.of(context));
    }

    public CostCategoryResponse calculateCosts(Driver driver, CostCalculationContext context, List<CostCalculationContext> dailyContexts) {

        List<AppliedCostResponse> costs = new ArrayList<>();

        // Salaire conducteur
        double annualSalary = driver.getAnnualSalary().doubleValue();
        int workingDays = workingDaysService.getWorkingDaysInYear(context.date().getYear());

        double dailySalary = annualSalary / workingDays;
        double salaryCost = dailySalary * (context.durationHours() / context.dailyDriverDurationHours());

        costs.add(new AppliedCostResponse(SALARY_LABEL, salaryCost));

        // CostParameters DRIVER
        costs.addAll(costParameterEngine.calculateCosts(CostParameterCategoryType.DRIVER,context,dailyContexts));

        double totalCost = costs.stream().mapToDouble(AppliedCostResponse::amount).sum();

        return new CostCategoryResponse(costs, totalCost);
    }

    public BigDecimal calculateSalaryForPeriod(Driver driver, LocalDate startDate, LocalDate endDateExclusive) {
        return calculateSalaryForPeriodExcludingDates(driver,startDate,endDateExclusive,Set.of());
    }

    public BigDecimal calculateSalaryForPeriodExcludingDates(Driver driver, LocalDate startDate, LocalDate endDateExclusive, Set<LocalDate> excludedDates) {
        BigDecimal annualSalary = driver.getAnnualSalary();
        if (annualSalary == null || !startDate.isBefore(endDateExclusive)) {
            return BigDecimal.ZERO;
        }

        BigDecimal salary = BigDecimal.ZERO;
        LocalDate date = startDate;

        while (date.isBefore(endDateExclusive)) {
            if (!excludedDates.contains(date) && date.getDayOfWeek().getValue() <= 5) {
                int workingDaysInYear = workingDaysService.getWorkingDaysInYear(date.getYear());
                salary = salary.add(annualSalary.divide(BigDecimal.valueOf(workingDaysInYear),2,java.math.RoundingMode.HALF_UP));
            }
            date = date.plusDays(1);
        }

        return salary;
    }
}