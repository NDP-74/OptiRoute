package com.optiroute.backend.entity.transport;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "transport_estimate")
public class TransportEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transport_id", nullable = false, unique = true)
    private Long transportId;

    @Column(name = "distance_meters", nullable = false)
    private Long distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    @Column(name = "polyline", nullable = false, columnDefinition = "TEXT")
    private String polyline;

    @Column(name = "estimated_fuel_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedFuelCost;

    @Column(name = "estimated_toll_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedTollCost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}