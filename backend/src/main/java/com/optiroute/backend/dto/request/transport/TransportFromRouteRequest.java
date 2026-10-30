package com.optiroute.backend.dto.request.transport;

import com.optiroute.backend.dto.response.route.RouteDto;
import jakarta.validation.Valid;

public record TransportFromRouteRequest(@Valid TransportRequest transport, RouteDto selectedRoute) {
}