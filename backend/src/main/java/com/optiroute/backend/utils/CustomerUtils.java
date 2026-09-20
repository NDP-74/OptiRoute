package com.optiroute.backend.utils;

import com.optiroute.backend.dto.response.CustomerLightResponse;
import com.optiroute.backend.dto.response.CustomerResponse;
import com.optiroute.backend.entity.Customer;

public final class CustomerUtils {

    private CustomerUtils() {
    }

    public static CustomerLightResponse toSummaryResponse(Customer customer) {
        return new CustomerLightResponse(customer.getId(), customer.getName(), customer.getCode(), customer.getCity());
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getExternalId(), customer.getExternalSource(), customer.getName(), customer.getCode(), customer.getAddress(),
            customer.getCity(), customer.getCountry(), customer.getCreatedAt(), customer.getUpdatedAt());
    }
}