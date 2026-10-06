package com.optiroute.backend.dto.response;

import java.time.LocalDateTime;

public record EventResponse(Long id, Long routeId, Long costParameterId, String costParameterLabel, Long pointOfInterestId, String pointOfInterestLabel, Long tractorId, Long semiTrailerId,
        LocalDateTime eventDate) {
}