package com.optiroute.backend.dto.response.vehicle;

import java.time.LocalDate;

public record SemiTrailerLightResponse(
        Long id,
        String registration,
        LocalDate depreciationStartDate,
        LocalDate depreciationEndDate) {
}
