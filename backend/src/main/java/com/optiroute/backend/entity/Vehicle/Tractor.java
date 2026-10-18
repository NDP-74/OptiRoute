package com.optiroute.backend.entity.vehicle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.optiroute.backend.entity.EntityUtils;

@Entity
@Table(name = "tractor")
@NoArgsConstructor
@Getter
@Setter
public class Tractor extends EntityUtils {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    private String registration;

    @Column(precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    private LocalDate depreciationStartDate;

    private LocalDate depreciationEndDate;

    @Column(name = "maintenance_cost_per_km", precision = 8, scale = 4)
    private BigDecimal maintenanceCostPerKm;
}