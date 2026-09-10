package com.optiroute.backend.dto.response.transport;

import java.util.List;

public record PlanningUnassignedVehiclesResponse(List<String> registrations, double depreciationCost) {
}
