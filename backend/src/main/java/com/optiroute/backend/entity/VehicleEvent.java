package com.optiroute.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vehicle_event")
@Getter
@Setter
public class VehicleEvent extends EntityUtils {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(length = 255)
    private String supplier;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tractor_id")
    private Tractor tractor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semi_trailer_id")
    private SemiTrailer semiTrailer;
}
