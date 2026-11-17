package com.optiroute.backend.dto.request.transport;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// Informations du service modifiables sans recalcul d'itinéraire
public record ServiceInfoRequest(
        @Size(max = 255) String name,
        Long customerId,
        @PositiveOrZero BigDecimal revenue,
        @PositiveOrZero Integer loadingDurationMinutes,
        @PositiveOrZero Integer unloadingDurationMinutes) {
}
