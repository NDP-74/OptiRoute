package com.optiroute.backend.dto.response.transport;

import java.util.List;

public record PlanningResponse(List<TransportPlanningResponse> transports, List<PlanningDriverResponse> drivers, PlanningUnassignedVehiclesResponse unassignedVehicles) {
}