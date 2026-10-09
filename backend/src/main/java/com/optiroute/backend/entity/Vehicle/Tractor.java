package com.optiroute.backend.entity.vehicle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.optiroute.backend.entity.EntityUtils;
import com.optiroute.backend.type.FuelType;

@Entity
@Table(name = "tractor")
@NoArgsConstructor
@Getter
@Setter
public class Tractor extends EntityUtils {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "external_source")
    private String externalSource = "MANUAL";

    @Column(nullable = false, unique = true, length = 15)
    private String registration;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String model;

    private Integer maxSpeed;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FuelType fuelType;

    @Column(precision = 6, scale = 2)
    private BigDecimal averageConsumption;

    @Column(precision = 6, scale = 2)
    private BigDecimal averageConsumptionEmpty;

    private Integer emptyWeightKg;

    private Integer grossCombinationWeightKg;

    private Integer heightCm;

    private Integer widthCm;

    private Integer lengthCm;

    private Integer axleCount;

    @Column(precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    private LocalDate depreciationStartDate;

    private LocalDate depreciationEndDate;

    @Column(name = "maintenance_cost_per_km", precision = 8, scale = 4)
    private BigDecimal maintenanceCostPerKm;
}