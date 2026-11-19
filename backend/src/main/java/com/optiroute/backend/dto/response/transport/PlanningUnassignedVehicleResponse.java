package com.optiroute.backend.dto.response.transport;

import java.math.BigDecimal;

public record PlanningUnassignedVehicleResponse(String registration, BigDecimal depreciationCost) {
}
