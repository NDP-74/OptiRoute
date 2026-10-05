package com.optiroute.backend.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PointOfInterestResponse(Long id, String label, String address, BigDecimal latitude, BigDecimal longitude, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
