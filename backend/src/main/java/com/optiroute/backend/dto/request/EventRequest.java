package com.optiroute.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record EventRequest(@NotNull Long serviceId, @NotNull Long costParameterId, Long pointOfInterestId, Long tractorId, Long semiTrailerId) {
}