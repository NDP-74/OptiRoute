package com.optiroute.backend.dto.response.transport;

import java.math.BigDecimal;
import java.util.List;

public record PlanningUnassignedVehiclesResponse(List<String> registrations, BigDecimal depreciationCost, List<PlanningUnassignedVehicleResponse> vehicles) {
}
