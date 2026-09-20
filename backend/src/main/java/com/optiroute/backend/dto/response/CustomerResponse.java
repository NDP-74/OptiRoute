package com.optiroute.backend.dto.response;

import java.time.OffsetDateTime;

public record CustomerResponse(Long id, String externalId, String externalSource, String name, String code, String address, String city, String country, OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}