package com.optiroute.backend.dto.request;

import com.fasterxml.jackson.databind.JsonNode;

public record CustomerRequest(String externalId, String externalSource, String name, String code, String address, String city, String country) {
}