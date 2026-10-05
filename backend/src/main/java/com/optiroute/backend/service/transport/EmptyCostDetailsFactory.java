package com.optiroute.backend.service.transport;

import java.util.List;

import org.springframework.stereotype.Component;

import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;

// Les coûts driver/vehicle/structure ne sont pas encore calculés pour une Route
@Component
class EmptyCostDetailsFactory {

    TransportCostDetailsResponse create() {
        return new TransportCostDetailsResponse(empty(), empty(), empty(), 0);
    }

    private CostCategoryResponse empty() {
        return new CostCategoryResponse(List.of(), 0);
    }
}
