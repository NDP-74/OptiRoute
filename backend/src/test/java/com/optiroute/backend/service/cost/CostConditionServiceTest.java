package com.optiroute.backend.service.cost;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

import com.optiroute.backend.dto.request.cost.CostCalculationContext;
import com.optiroute.backend.dto.request.cost.CostConditionRequest;
import com.optiroute.backend.entity.cost.CostCondition;
import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.repository.cost.CostConditionRepository;
import com.optiroute.backend.type.cost.CostConditionFieldType;
import com.optiroute.backend.type.cost.CostConditionOperatorType;
import com.optiroute.backend.type.cost.CostConditionSourceType;
import com.optiroute.backend.type.cost.CostParameterAssignmentType;

class CostConditionServiceTest {

    private final CostConditionService service = new CostConditionService(mock(CostConditionRepository.class));

    @ParameterizedTest
    @CsvSource({
        // heure, décalage en jours, opérateur, seuil, attendu
        "01:30, 1, AFTER, 20:00, true",
        "01:30, 1, BEFORE, 20:00, false",
        "01:30, 1, EQUALS, 01:30, false",
        "01:30, 0, EQUALS, 01:30, true",
        "18:00, 0, AFTER, 20:00, false",
        "21:00, 0, AFTER, 20:00, true"
    })
    void shouldCompareDayEndAfterMidnightAsALaterTimeOfTheDepartureDay(String time, int offset, CostConditionOperatorType operator,
            String threshold, boolean expected) {
        CostParameter parameter = parameter();
        CostCondition dayEnd = condition(CostConditionFieldType.DRIVER_DAY_END_TIME, operator, threshold);
        CostCondition arrival = condition(CostConditionFieldType.ARRIVAL_TIME, operator, threshold);

        CostCalculationContext context = context(LocalTime.parse(time), offset);

        assertEquals(expected, service.isApplicable(parameter, context, List.of(dayEnd)));
        assertEquals(expected, service.isApplicable(parameter, context, List.of(arrival)));
    }

    @Test
    void shouldIgnoreAnInvalidStoredConditionInsteadOfFailingTheCalculation() {
        CostCondition invalid = condition(CostConditionFieldType.DISTANCE, CostConditionOperatorType.BEFORE, "abc");

        assertFalse(service.isApplicable(parameter(), context(LocalTime.NOON, 0), List.of(invalid)));
    }

    @ParameterizedTest
    @CsvSource({
        "TRIP, DISTANCE, BEFORE, 10",
        "TRIP, DISTANCE, GREATER_THAN, abc",
        "TRIP, ARRIVAL_TIME, GREATER_THAN, 20:00",
        "TRIP, ARRIVAL_TIME, AFTER, 25:00",
        "TRIP, EMPTY_TRIP, EQUALS, oui",
        "VEHICLE, VEHICLE_TYPE, EQUALS, abc",
        "DRIVER, DISTANCE, GREATER_THAN, 10",
        "TRIP, DISTANCE, GREATER_THAN, ' '"
    })
    void shouldRejectInvalidConditions(CostConditionSourceType source, CostConditionFieldType field, CostConditionOperatorType operator, String value) {
        assertThrows(ResponseStatusException.class,
            () -> CostConditionService.validate(new CostConditionRequest(source, field, operator, value)));
    }

    @ParameterizedTest
    @CsvSource({
        "TRIP, DISTANCE, GREATER_THAN, 10.5",
        "TRIP, ARRIVAL_TIME, AFTER, 20:00",
        "TRIP, LOADED_TRIP, NOT_EQUALS, false",
        "VEHICLE, VEHICLE_TYPE, EQUALS, 3",
        "DRIVER, DRIVER_ENDS_DAY_AT_PARKING, EQUALS, true"
    })
    void shouldAcceptValidConditions(CostConditionSourceType source, CostConditionFieldType field, CostConditionOperatorType operator, String value) {
        assertDoesNotThrow(() -> CostConditionService.validate(new CostConditionRequest(source, field, operator, value)));
    }

    private CostParameter parameter() {
        CostParameter parameter = new CostParameter();
        parameter.setAssignmentType(CostParameterAssignmentType.ALL_CONDITIONS);
        return parameter;
    }

    private CostCondition condition(CostConditionFieldType field, CostConditionOperatorType operator, String value) {
        CostCondition condition = new CostCondition();
        condition.setField(field);
        condition.setOperator(operator);
        condition.setValue(value);
        return condition;
    }

    private CostCalculationContext context(LocalTime time, int dayOffset) {
        return new CostCalculationContext(LocalDate.of(2026, 11, 3), 100, 100, 2, 1, false, LocalTime.of(8, 0), time,
            LocalTime.of(8, 0), time, null, 2, 2, null, 1, dayOffset, dayOffset);
    }
}
