package com.optiroute.backend.entity.cost;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import com.optiroute.backend.type.cost.CostParameterAssignmentType;
import com.optiroute.backend.type.cost.CostParameterCategoryType;
import com.optiroute.backend.type.cost.CostParameterUnitType;

@Entity
@Getter
@Setter
@Table(name = "cost_parameter")
public class CostParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CostParameterCategoryType category;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CostParameterUnitType unit;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 20)
    private CostParameterAssignmentType assignmentType = CostParameterAssignmentType.AUTOMATIC;
}