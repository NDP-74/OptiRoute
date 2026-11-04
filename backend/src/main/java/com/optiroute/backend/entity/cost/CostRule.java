package com.optiroute.backend.entity.cost;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import com.optiroute.backend.type.cost.CostRuleLogicalOperatorType;

@Entity
@Table(name = "cost_rule")
@Getter
@Setter
public class CostRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cost_parameter_id", nullable = false)
    private Long costParameterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "logical_operator", nullable = false, length = 10)
    private CostRuleLogicalOperatorType logicalOperator;
}