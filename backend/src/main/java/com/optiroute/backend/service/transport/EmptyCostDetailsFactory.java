package com.optiroute.backend.service.transport;

import org.springframework.stereotype.Component;

import com.optiroute.backend.dto.response.cost.CostCategoryResponse;
import com.optiroute.backend.dto.response.cost.TransportCostDetailsResponse;

// Les coûts driver/vehicle/structure ne sont pas encore calculés pour une Route
@Component
class EmptyCostDetailsFactory {

    TransportCostDetailsResponse create() {
        return TransportCostDetailsResponse.of(CostCategoryResponse.empty(), CostCategoryResponse.empty(), CostCategoryResponse.empty());
    }
}
