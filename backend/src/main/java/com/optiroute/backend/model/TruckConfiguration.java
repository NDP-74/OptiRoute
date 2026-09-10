package com.optiroute.backend.model;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TruckConfiguration {

    private Integer weightKg;

    private Integer heightCm;
    private Integer widthCm;
    private Integer lengthCm;

    private Integer axleCount;

    private Integer maxSpeed;
    private BigDecimal averageConsumption;
}