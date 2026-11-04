package com.optiroute.backend.service.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.optiroute.backend.dto.request.cost.CostCalculationContext;
import com.optiroute.backend.entity.cost.CostCondition;
import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.cost.CostParameterRepository;
import com.optiroute.backend.type.cost.CostConditionFieldType;
import com.optiroute.backend.type.cost.CostConditionOperatorType;
import com.optiroute.backend.type.cost.CostParameterCategoryType;
import com.optiroute.backend.type.cost.CostParameterUnitType;

class VehicleCostServiceTest {

    @Test
    void shouldUseWorkingDaysInDepreciationPeriodForVehicleDepreciation() throws Exception {
        VehicleCostService service = new VehicleCostService(null, new WorkingDaysService());

        Tractor tractor = new Tractor();
        tractor.setPurchaseCost(new BigDecimal("1000"));
        tractor.setDepreciationStartDate(LocalDate.of(2024,1,1));
        tractor.setDepreciationEndDate(LocalDate.of(2024,1,7));

        SemiTrailer semiTrailer = new SemiTrailer();
        semiTrailer.setPurchaseCost(new BigDecimal("500"));
        semiTrailer.setDepreciationStartDate(LocalDate.of(2024,1,1));
        semiTrailer.setDepreciationEndDate(LocalDate.of(2024,1,7));

        Method method = VehicleCostService.class.getDeclaredMethod("calculateDepreciation",Tractor.class,SemiTrailer.class,double.class,double.class,LocalDate.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service,tractor,semiTrailer,100.0,200.0,LocalDate.of(2024,1,5));

        assertEquals(150.0,result,0.0001);
    }

    @Test
    void shouldAllocateDailyCostAcrossTripsOfTheSameDay() {
        CostParameterRepository repository = mock(CostParameterRepository.class);
        CostRuleService costRuleService = mock(CostRuleService.class);
        CostParameterEngine engine = new CostParameterEngine(repository, costRuleService, new WorkingDaysService());

        CostParameter parameter = new CostParameter();
        parameter.setId(1L);
        parameter.setCategory(CostParameterCategoryType.VEHICLE);
        parameter.setLabel("Frais journaliers");
        parameter.setValue(new BigDecimal("300"));
        parameter.setUnit(CostParameterUnitType.EUR_PER_DAY);
        parameter.setActive(true);

        when(repository.findByCategory(CostParameterCategoryType.VEHICLE)).thenReturn(List.of(parameter));
        when(costRuleService.isApplicable(parameter,new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0)))
            .thenReturn(true);

        CostCalculationContext context = new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0);

        double result = engine.calculateCosts(CostParameterCategoryType.VEHICLE,context).getFirst().amount();

        assertEquals(100.0,result,0.0001);
    }

    @Test
    void shouldApplyConditionalDailyCostToEveryTripWhenOneTripMatches() {
        CostParameterRepository repository = mock(CostParameterRepository.class);
        CostRuleService costRuleService = mock(CostRuleService.class);
        CostParameterEngine engine = new CostParameterEngine(repository, costRuleService, new WorkingDaysService());

        CostParameter parameter = new CostParameter();
        parameter.setId(2L);
        parameter.setCategory(CostParameterCategoryType.VEHICLE);
        parameter.setLabel("Coût conditionnel journalier");
        parameter.setValue(new BigDecimal("300"));
        parameter.setUnit(CostParameterUnitType.EUR_PER_DAY);
        parameter.setActive(true);

        CostCalculationContext matchingTrip = new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, true, null, null, null, null, null, 5.0);
        CostCalculationContext nonMatchingTrip = new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0);

        when(repository.findByCategory(CostParameterCategoryType.VEHICLE)).thenReturn(List.of(parameter));
        when(costRuleService.isApplicable(parameter,matchingTrip)).thenReturn(true);
        when(costRuleService.isApplicable(parameter,nonMatchingTrip)).thenReturn(false);

        double result = engine.calculateCosts(CostParameterCategoryType.VEHICLE,nonMatchingTrip,List.of(matchingTrip,nonMatchingTrip)).getFirst().amount();

        assertEquals(100.0,result,0.0001);
    }

    @Test
    void shouldHandleNullVehicleTypeWhenEvaluatingStringCondition() throws Exception {
        CostRuleService service = new CostRuleService(null, null);

        CostCondition condition = new CostCondition();
        condition.setField(CostConditionFieldType.VEHICLE_TYPE);
        condition.setOperator(CostConditionOperatorType.EQUALS);
        condition.setValue("42");

        CostCalculationContext context = new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0);

        Method method = CostRuleService.class.getDeclaredMethod("evaluateCondition",CostCondition.class,CostCalculationContext.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(service,condition,context);

        assertEquals(false,result);
    }
}
