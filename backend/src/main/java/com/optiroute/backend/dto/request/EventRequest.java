package com.optiroute.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record EventRequest(@NotNull Long serviceId, Long routeId, @NotNull Long costParameterId, Long pointOfInterestId, Long tractorId, Long semiTrailerId,
        @PositiveOrZero Integer durationMinutes) {
}