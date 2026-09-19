package com.optiroute.backend.service.route;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.response.route.RouteCostDetailsDto;
import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.service.cost.FuelPriceService;
import com.optiroute.backend.service.vehicle.SemiTrailerService;
import com.optiroute.backend.service.vehicle.TractorService;

import lombok.RequiredArgsConstructor;

import com.optiroute.backend.model.TruckConfiguration;
import com.optiroute.backend.mapper.TruckConfigurationFactory;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteOptimizationService {

    private final RoutingService routingService;
    private final RoutePtvParser routePtvParser;
    private final RouteCostService routeCostService;
    private final FuelPriceService fuelPriceService;
    private final TractorService tractorService;
    private final SemiTrailerService semiTrailerService;
    private final TruckConfigurationFactory truckConfigurationFactory;

    public RoutesResponse calculateRoute(RouteRequest request) {

        TruckConfiguration truckConfiguration;
        if (request.getTractorId() != null && request.getSemiTrailerId() != null) {
            Tractor tractor = tractorService.getEntityById(request.getTractorId());
            SemiTrailer semiTrailer = semiTrailerService.getEntityById(request.getSemiTrailerId());
            truckConfiguration = truckConfigurationFactory.create(tractor,semiTrailer,request.isEmptyTrip());
        } else {
            truckConfiguration = truckConfigurationFactory.createDefault();
        }

        // PTV Routing API + Parsing
        String raw = routingService.calculateRoutes(request,truckConfiguration);
        List<RoutePtvParser.ParsedRoute> parsedRoutes = routePtvParser.parseRoutes(raw);

        // Cost calculation
        double fuelPrice = fuelPriceService.getAverageDieselPrice();
        double consumption = truckConfiguration.getAverageConsumption().doubleValue();

        // Enriched DTOs
        List<RouteDto> routes = new ArrayList<>();
        for (RoutePtvParser.ParsedRoute parsed : parsedRoutes) {
            double km = parsed.distanceMeters / 1000.0;
            RouteCostDetailsDto costs = routeCostService.calculateCosts(km,consumption,fuelPrice,parsed.tollCost);

            RouteDto dto = new RouteDto();
            dto.setDistanceMeters(parsed.distanceMeters);
            dto.setDuration(parsed.duration);
            dto.setBaseDuration(parsed.baseDuration);
            dto.setPolyline(parsed.polyline);
            dto.setCosts(costs);

            dto.setOriginLat(request.getOrigin().getLat());
            dto.setOriginLng(request.getOrigin().getLng());
            dto.setDestinationLat(request.getDestination().getLat());
            dto.setDestinationLng(request.getDestination().getLng());

            routes.add(dto);
        }

        // Response
        RoutesResponse response = new RoutesResponse();
        response.setRoutes(routes);

        return response;
    }
}