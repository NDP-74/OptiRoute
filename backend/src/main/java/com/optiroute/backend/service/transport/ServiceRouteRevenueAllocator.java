package com.optiroute.backend.service.transport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import com.optiroute.backend.entity.transport.Route;

final class ServiceRouteRevenueAllocator {

    private ServiceRouteRevenueAllocator() {
    }

    static BigDecimal allocate(BigDecimal serviceRevenue, List<Route> serviceRoutes, Route route) {
        if (serviceRevenue == null) {
            return null;
        }
        if (serviceRoutes.size() <= 1) {
            return serviceRevenue;
        }

        long totalDistance = serviceRoutes.stream().mapToLong(Route::getDistanceMeters).sum();
        if (totalDistance <= 0) {
            throw new IllegalStateException("Cannot distribute service revenue across zero-distance routes");
        }

        long totalCents = serviceRevenue.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        long remainingCents = totalCents;
        List<Route> orderedRoutes = serviceRoutes.stream().sorted(Comparator.comparing(Route::getId)).toList();
        for (int index = 0; index < orderedRoutes.size(); index++) {
            Route current = orderedRoutes.get(index);
            long allocatedCents = index == orderedRoutes.size() - 1
                ? remainingCents
                : BigDecimal.valueOf(totalCents).multiply(BigDecimal.valueOf(current.getDistanceMeters()))
                    .divide(BigDecimal.valueOf(totalDistance), 0, RoundingMode.DOWN).longValueExact();
            if (current.getId().equals(route.getId())) {
                return BigDecimal.valueOf(allocatedCents, 2);
            }
            remainingCents -= allocatedCents;
        }
        throw new IllegalArgumentException("Route does not belong to the service route list");
    }
}
