package com.optiroute.backend.service.transport;

import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.dto.response.route.RouteDto;

record TransportRouteSegment(TransportRequest request, RouteDto route, boolean calculatedTimes) {
}
