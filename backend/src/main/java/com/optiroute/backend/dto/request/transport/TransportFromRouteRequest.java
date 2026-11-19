package com.optiroute.backend.dto.request.transport;

import com.optiroute.backend.dto.response.route.RouteDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TransportFromRouteRequest(@NotNull @Valid TransportRequest transport, @NotNull RouteDto selectedRoute) {
}