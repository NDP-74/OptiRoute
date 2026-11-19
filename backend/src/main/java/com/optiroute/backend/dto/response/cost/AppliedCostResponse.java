package com.optiroute.backend.dto.response.cost;

import java.math.BigDecimal;

import com.optiroute.backend.utils.MoneyUtils;

public record AppliedCostResponse(String label, BigDecimal amount) {

    public AppliedCostResponse {
        amount = MoneyUtils.round(amount);
    }
}
