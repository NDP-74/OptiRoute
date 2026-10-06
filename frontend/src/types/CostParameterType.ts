export type CostParameterCategory =
    | "VEHICLE"
    | "DRIVER"
    | "STRUCTURE"

export type CostParameterAssignmentType =
    | "AUTOMATIC"
    | "MANUAL"

export type CostParameterUnit =
    | "EUR_PER_KM"
    | "EUR_PER_TRIP"
    | "EUR_PER_HOUR"
    | "EUR_PER_DAY"
    | "EUR_PER_MONTH"
    | "EUR_PER_YEAR"

export type CostConditionSource =
    | "TRIP"
    | "VEHICLE"
    | "DRIVER"

export type CostConditionField =
    | "DISTANCE"
    | "DURATION"
    | "DEPARTURE_TIME"
    | "ARRIVAL_TIME"
    | "EMPTY_TRIP"
    | "LOADED_TRIP"
    | "VEHICLE_TYPE"
    | "DRIVER_DAY_START_TIME"
    | "DRIVER_DAY_END_TIME"

export type CostConditionOperator =
    | "EQUALS"
    | "NOT_EQUALS"
    | "LESS_THAN"
    | "GREATER_THAN"
    | "BEFORE"
    | "AFTER"

export type CostRuleLogicalOperator =
    | "AND"
    | "OR"