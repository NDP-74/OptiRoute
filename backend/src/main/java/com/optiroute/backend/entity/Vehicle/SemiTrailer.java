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

    @Column(nullable = false, unique = true, length = 15)
    private String registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trailer_type_id")
    private TrailerType trailerType;

    @Column(precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    private LocalDate depreciationStartDate;

    private LocalDate depreciationEndDate;

    @Column(name = "maintenance_cost_per_km", precision = 8, scale = 4)
    private BigDecimal maintenanceCostPerKm;
}