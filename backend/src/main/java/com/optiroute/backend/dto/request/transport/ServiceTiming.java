package com.optiroute.backend.dto.request.transport;

import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;

public record ServiceTiming(
        @PositiveOrZero Integer loadingDurationMinutes,
        @PositiveOrZero Integer unloadingDurationMinutes,
        OffsetDateTime departureStart, OffsetDateTime departureEnd,
        OffsetDateTime arrivalStart, OffsetDateTime arrivalEnd) {

    @AssertTrue(message = "Service time window start must not be after its end")
    @JsonIgnore
    public boolean isTimeWindowValid() {
        return (departureStart == null || departureEnd == null || !departureStart.isAfter(departureEnd))
            && (arrivalStart == null || arrivalEnd == null || !arrivalStart.isAfter(arrivalEnd));
    }
}
