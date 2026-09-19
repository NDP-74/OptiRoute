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
@Table(name = "transport")
public class Transport extends EntityUtils {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalId;
    private String externalSource = "MANUAL";

    private String name;
    private String status = "PLANNED";

    private Long driverId;
    private Long tractorId;
    private Long semiTrailerId;
    private Long customerId;

    private OffsetDateTime plannedStart;
    private OffsetDateTime plannedEnd;

    private OffsetDateTime actualStart;
    private OffsetDateTime actualEnd;

    private String originName;
    private String originAddress;
    private double originLat;
    private double originLng;

    private String destinationName;
    private String destinationAddress;
    private double destinationLat;
    private double destinationLng;

    private boolean emptyTrip;

    @Column(precision = 12, scale = 2)
    private BigDecimal revenue;
}