package com.optiroute.backend.entity.vehicle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.optiroute.backend.entity.EntityUtils;

@Entity
@Table(name = "semi_trailer")
@Getter
@Setter
public class SemiTrailer extends EntityUtils {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trailer_type_id")
    private TrailerType trailerType;

    private Integer maxSpeed;

    private Integer emptyWeightKg;

    private Integer grossVehicleWeightKg;

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