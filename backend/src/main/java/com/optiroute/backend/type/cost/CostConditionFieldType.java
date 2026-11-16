package com.optiroute.backend.type.cost;

public enum CostConditionFieldType {
    // Trip
    DISTANCE, DURATION, DEPARTURE_TIME, ARRIVAL_TIME, EMPTY_TRIP, LOADED_TRIP,

    // Vehicle
    VEHICLE_TYPE,

    // Driver
    DRIVER_DAY_START_TIME, DRIVER_DAY_END_TIME, DRIVER_ENDS_DAY_AT_PARKING
}