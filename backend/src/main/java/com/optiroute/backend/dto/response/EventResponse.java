package com.optiroute.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponse(Long id, Long routeId, Long costParameterId, String costParameterLabel, Long pointOfInterestId, String pointOfInterestLabel, Long tractorId, Long semiTrailerId,
        BigDecimal pointOfInterestLatitude, BigDecimal pointOfInterestLongitude, LocalDateTime eventDate) {
}