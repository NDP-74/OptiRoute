package com.optiroute.backend.entity.driver;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import com.optiroute.backend.entity.EntityUtils;
import com.optiroute.backend.entity.vehicle.SemiTrailer;
import com.optiroute.backend.entity.vehicle.Tractor;

import com.optiroute.backend.type.driver.DriverCostType;
import com.optiroute.backend.type.driver.DriverType;

@Entity
@Table(name = "driver")
@Getter
@Setter
public class Driver extends EntityUtils {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String login;

    private String phoneNumber;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "annual_salary", precision = 10, scale = 2)
    private BigDecimal annualSalary;

    @Column(name = "monthly_working_hours", precision = 6, scale = 2)
    private BigDecimal monthlyWorkingHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tractor_id")
    private Tractor tractor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semi_trailer_id")
    private SemiTrailer semiTrailer;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", nullable = false, length = 30)
    private DriverCostType costType;

    @Enumerated(EnumType.STRING)
    @Column(name = "driver_type", nullable = false, length = 30)
    private DriverType driverType;

    @Column(name = "location_label", length = 255)
    private String locationLabel;

    @Column(name = "location_latitude", precision = 10, scale = 7)
    private BigDecimal locationLatitude;

    @Column(name = "location_longitude", precision = 10, scale = 7)
    private BigDecimal locationLongitude;
}