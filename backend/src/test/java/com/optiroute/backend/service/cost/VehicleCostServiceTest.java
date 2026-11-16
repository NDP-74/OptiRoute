package com.optiroute.backend.service.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.optiroute.backend.dto.request.cost.CostCalculationContext;
import com.optiroute.backend.entity.cost.CostCondition;
import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.repository.cost.CostConditionRepository;
import com.optiroute.backend.repository.cost.CostParameterRepository;
import com.optiroute.backend.type.cost.CostConditionFieldType;
import com.optiroute.backend.type.cost.CostConditionOperatorType;
import com.optiroute.backend.type.cost.CostParameterAssignmentType;
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
        CostConditionService costConditionService = mock(CostConditionService.class);
        CostParameterEngine engine = new CostParameterEngine(repository, costConditionService, new WorkingDaysService());

        CostParameter parameter = new CostParameter();
        parameter.setId(1L);
        parameter.setCategory(CostParameterCategoryType.VEHICLE);
        parameter.setLabel("Frais journaliers");
        parameter.setValue(new BigDecimal("300"));
        parameter.setUnit(CostParameterUnitType.EUR_PER_DAY);
        parameter.setActive(true);

        when(repository.findByCategory(CostParameterCategoryType.VEHICLE)).thenReturn(List.of(parameter));
        when(costConditionService.isApplicable(parameter,new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0)))
            .thenReturn(true);

        CostCalculationContext context = new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0);

        double result = engine.calculateCosts(CostParameterCategoryType.VEHICLE,context).getFirst().amount();

        assertEquals(100.0,result,0.0001);
    }

    @Test
    void shouldApplyConditionalDailyCostToEveryTripWhenOneTripMatches() {
        CostParameterRepository repository = mock(CostParameterRepository.class);
        CostConditionService costConditionService = mock(CostConditionService.class);
        CostParameterEngine engine = new CostParameterEngine(repository, costConditionService, new WorkingDaysService());

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
        when(costConditionService.isApplicable(parameter,matchingTrip)).thenReturn(true);
        when(costConditionService.isApplicable(parameter,nonMatchingTrip)).thenReturn(false);

        double result = engine.calculateCosts(CostParameterCategoryType.VEHICLE,nonMatchingTrip,List.of(matchingTrip,nonMatchingTrip)).getFirst().amount();

        assertEquals(100.0,result,0.0001);
    }

    @Test
    void shouldRequireEveryConditionWhenUsingAllConditionsLogic() {
        CostConditionRepository repository = mock(CostConditionRepository.class);
        CostConditionService service = new CostConditionService(repository);

        CostParameter parameter = new CostParameter();
        parameter.setId(3L);
        parameter.setAssignmentType(CostParameterAssignmentType.ALL_CONDITIONS);

        CostCondition matchingCondition = numericCondition(CostConditionOperatorType.GREATER_THAN, "50");
        CostCondition nonMatchingCondition = numericCondition(CostConditionOperatorType.LESS_THAN, "50");
        when(repository.findByCostParameterId(3L)).thenReturn(List.of(matchingCondition, nonMatchingCondition));

        CostCalculationContext context = new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0);
        assertFalse(service.isApplicable(parameter, context));
    }

    @Test
    void shouldRequireAtLeastOneConditionWhenUsingAnyConditionLogic() {
        CostConditionRepository repository = mock(CostConditionRepository.class);
        CostConditionService service = new CostConditionService(repository);

        CostParameter parameter = new CostParameter();
        parameter.setId(4L);
        parameter.setAssignmentType(CostParameterAssignmentType.ANY_CONDITION);

        CostCondition matchingCondition = numericCondition(CostConditionOperatorType.GREATER_THAN, "50");
        CostCondition nonMatchingCondition = numericCondition(CostConditionOperatorType.LESS_THAN, "50");
        when(repository.findByCostParameterId(4L)).thenReturn(List.of(matchingCondition, nonMatchingCondition));

        CostCalculationContext context = new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0);
        assertTrue(service.isApplicable(parameter, context));
    }

    @Test
    void shouldTreatNoConditionsAsAlwaysApplicable() {
        CostConditionRepository repository = mock(CostConditionRepository.class);
        CostConditionService service = new CostConditionService(repository);
        CostParameter parameter = new CostParameter();
        parameter.setId(5L);
        parameter.setAssignmentType(CostParameterAssignmentType.ALL_CONDITIONS);

        when(repository.findByCostParameterId(5L)).thenReturn(List.of());

        CostCalculationContext context = new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0);
        assertTrue(service.isApplicable(parameter, context));
    }

    @Test
    void shouldHandleNullVehicleTypeWhenEvaluatingCondition() {
        CostConditionRepository repository = mock(CostConditionRepository.class);
        CostConditionService service = new CostConditionService(repository);

        CostParameter parameter = new CostParameter();
        parameter.setId(6L);
        parameter.setAssignmentType(CostParameterAssignmentType.ALL_CONDITIONS);
        CostCondition condition = new CostCondition();
        condition.setField(CostConditionFieldType.VEHICLE_TYPE);
        condition.setOperator(CostConditionOperatorType.EQUALS);
        condition.setValue("42");
        when(repository.findByCostParameterId(6L)).thenReturn(List.of(condition));

        CostCalculationContext context = new CostCalculationContext(LocalDate.of(2024,1,5), 100.0, 500.0, 3.0, 3, false, null, null, null, null, null, 5.0);
        assertFalse(service.isApplicable(parameter, context));
    }

    @Test
    void shouldDefaultToAlwaysApplicableWithoutEvaluatingConditions() {
        CostConditionRepository repository = mock(CostConditionRepository.class);
        CostConditionService service = new CostConditionService(repository);
        CostParameter parameter = new CostParameter();

        assertEquals(CostParameterAssignmentType.ALWAYS, parameter.getAssignmentType());
        assertTrue(service.isApplicable(parameter, null));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldNeverApplyManualCostAutomatically() {
        CostConditionRepository repository = mock(CostConditionRepository.class);
        CostConditionService service = new CostConditionService(repository);
        CostParameter parameter = new CostParameter();
        parameter.setAssignmentType(CostParameterAssignmentType.MANUAL);

        assertFalse(service.isApplicable(parameter, null));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({
        "true, EQUALS, true, true",
        "true, EQUALS, false, false",
        "false, EQUALS, false, true",
        "false, EQUALS, true, false",
        "true, NOT_EQUALS, false, true",
        "false, NOT_EQUALS, false, false",
        ", EQUALS, true, false",
        ", EQUALS, false, false",
        ", NOT_EQUALS, true, false",
        ", NOT_EQUALS, false, false"
    })
    void shouldEvaluateDriverParkingConditionWithoutTreatingUnknownAsFalse(Boolean actual, CostConditionOperatorType operator, String expected, boolean applicable) {
        CostConditionRepository repository = mock(CostConditionRepository.class);
        CostConditionService service = new CostConditionService(repository);
        CostParameter parameter = new CostParameter();
        parameter.setId(7L);
        parameter.setAssignmentType(CostParameterAssignmentType.ALL_CONDITIONS);
        CostCondition condition = new CostCondition();
        condition.setField(CostConditionFieldType.DRIVER_ENDS_DAY_AT_PARKING);
        condition.setOperator(operator);
        condition.setValue(expected);
        when(repository.findByCostParameterId(7L)).thenReturn(List.of(condition));

        CostCalculationContext context = new CostCalculationContext(LocalDate.of(2026,11,16), 100, 100, 1, 1, false,
            null, null, null, null, null, 1, 1, actual);

        assertEquals(applicable, service.isApplicable(parameter, context));
    }

    @ParameterizedTest
    @CsvSource({"false, 20, 0, 60", "true, 50, 30, 120"})
    void shouldAllocateNightAcrossTwoDaysAndAccumulateConsecutiveNights(boolean tuesdayTriggers, double tuesdayAmount, double wednesdayAmount, double total) {
        CostParameterRepository repository = mock(CostParameterRepository.class);
        CostConditionService conditions = mock(CostConditionService.class);
        CostParameterEngine engine = new CostParameterEngine(repository, conditions, new WorkingDaysService());
        CostParameter parameter = new CostParameter();
        parameter.setLabel("Nuit");
        parameter.setValue(new BigDecimal("60"));
        parameter.setUnit(CostParameterUnitType.EUR_PER_NIGHT);
        parameter.setAssignmentType(CostParameterAssignmentType.ALL_CONDITIONS);
        when(repository.findByCategory(CostParameterCategoryType.DRIVER)).thenReturn(List.of(parameter));
        when(conditions.isApplicable(org.mockito.ArgumentMatchers.eq(parameter),org.mockito.ArgumentMatchers.any()))
            .thenAnswer(invocation -> invocation.getArgument(1,CostCalculationContext.class).emptyTrip());
        LocalDate monday = LocalDate.of(2026,11,16);
        CostCalculationContext mondayFirst = nightContext(monday,2,3,true);
        CostCalculationContext mondaySecond = nightContext(monday,2,3,true);
        CostCalculationContext tuesday = nightContext(monday.plusDays(1),1,2,tuesdayTriggers);
        CostCalculationContext wednesday = nightContext(monday.plusDays(2),1,1,false);
        List<CostCalculationContext> contexts = List.of(mondayFirst,mondaySecond,tuesday,wednesday);

        double mondayCost = engine.calculateCosts(CostParameterCategoryType.DRIVER,mondayFirst,contexts).getFirst().amount();
        double tuesdayCost = engine.calculateCosts(CostParameterCategoryType.DRIVER,tuesday,contexts).getFirst().amount();
        var wednesdayCosts = engine.calculateCosts(CostParameterCategoryType.DRIVER,wednesday,contexts);
        double wednesdayCost = wednesdayCosts.stream().mapToDouble(c -> c.amount()).sum();

        assertEquals(20,mondayCost,0.0001);
        assertEquals(tuesdayAmount,tuesdayCost,0.0001);
        assertEquals(wednesdayAmount,wednesdayCost,0.0001);
        assertEquals(total,2 * mondayCost + tuesdayCost + wednesdayCost,0.0001);
    }

    @Test
    void shouldAllocateNightWithoutFollowingTripsAndKeepManualCostOnEventTrip() {
        CostParameterRepository repository = mock(CostParameterRepository.class);
        CostConditionService conditions = mock(CostConditionService.class);
        CostParameterEngine engine = new CostParameterEngine(repository,conditions,new WorkingDaysService());
        CostParameter parameter = new CostParameter();
        parameter.setValue(new BigDecimal("60"));
        parameter.setUnit(CostParameterUnitType.EUR_PER_NIGHT);
        when(repository.findByCategory(CostParameterCategoryType.VEHICLE)).thenReturn(List.of(parameter));
        CostCalculationContext context = nightContext(LocalDate.of(2026,11,16),2,2,true);
        when(conditions.isApplicable(parameter,context)).thenReturn(true);

        assertEquals(30,engine.calculateCosts(CostParameterCategoryType.VEHICLE,context,List.of(context,context)).getFirst().amount(),0.0001);
        parameter.setAssignmentType(CostParameterAssignmentType.MANUAL);
        assertTrue(engine.calculateCosts(CostParameterCategoryType.VEHICLE,context,List.of(context)).isEmpty());
        assertEquals(20,engine.calculateManualCost(parameter,nightContext(context.date(),2,3,true)).amount(),0.0001);
    }

    @Test
    void shouldNotTriggerDailyCostFromPreviousDay() {
        CostParameterRepository repository = mock(CostParameterRepository.class);
        CostConditionService conditions = mock(CostConditionService.class);
        CostParameterEngine engine = new CostParameterEngine(repository,conditions,new WorkingDaysService());
        CostParameter parameter = new CostParameter();
        parameter.setValue(new BigDecimal("60"));
        parameter.setUnit(CostParameterUnitType.EUR_PER_DAY);
        when(repository.findByCategory(CostParameterCategoryType.VEHICLE)).thenReturn(List.of(parameter));
        CostCalculationContext previous = nightContext(LocalDate.of(2026,11,16),1,2,true);
        CostCalculationContext current = nightContext(previous.date().plusDays(1),1,1,false);
        when(conditions.isApplicable(parameter,previous)).thenReturn(true);
        when(conditions.isApplicable(parameter,current)).thenReturn(false);

        assertTrue(engine.calculateCosts(CostParameterCategoryType.VEHICLE,current,List.of(previous,current)).isEmpty());
    }

    private CostCalculationContext nightContext(LocalDate date, int dailyCount, int nightCount, boolean triggers) {
        return new CostCalculationContext(date,100,100,1,dailyCount,triggers,null,null,null,null,null,1,1,null,nightCount);
    }

    private CostCondition numericCondition(CostConditionOperatorType operator, String value) {
        CostCondition condition = new CostCondition();
        condition.setField(CostConditionFieldType.DISTANCE);
        condition.setOperator(operator);
        condition.setValue(value);
        return condition;
    }
}
