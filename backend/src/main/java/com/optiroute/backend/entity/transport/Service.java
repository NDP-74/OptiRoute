package com.optiroute.backend.entity.transport;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import com.optiroute.backend.entity.EntityUtils;

@Entity
@Getter
@Setter
@Table(name = "service")
public class Service extends EntityUtils {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalId;
    private String externalSource = "MANUAL";

    private String name;
    private String status = "PLANNED";

    private Long customerId;

    @Column(precision = 12, scale = 2)
    private BigDecimal revenue;
}