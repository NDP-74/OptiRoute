package com.optiroute.backend.entity.transport;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.optiroute.backend.entity.EntityUtils;

@Entity
@Getter
@Setter
@Table(name = "route")
public class Route extends EntityUtils {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    private Long driverId;
    private Long tractorId;
    private Long semiTrailerId;

    private OffsetDateTime startDate;
    private OffsetDateTime endDate;

    private String originName;
    private String originAddress;
    private double originLat;
    private double originLng;

    private String destinationName;
    private String destinationAddress;
    private double destinationLat;
    private double destinationLng;

    private boolean emptyTrip;

    @Column(nullable = false)
    private Long distanceMeters;

    @Column(nullable = false)
    private Long durationSeconds;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String polyline;

    @Column(precision = 10, scale = 2)
    private BigDecimal fuelCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal tollCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal driverCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal vehicleCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal structureCost;
}