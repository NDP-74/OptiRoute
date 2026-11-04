package com.optiroute.backend.entity.cost;

import com.optiroute.backend.type.cost.CostConditionFieldType;
import com.optiroute.backend.type.cost.CostConditionOperatorType;
import com.optiroute.backend.type.cost.CostConditionSourceType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cost_condition")
@Getter
@Setter
public class CostCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_parameter_id", nullable = false)
    private CostParameter costParameter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CostConditionSourceType source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CostConditionFieldType field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CostConditionOperatorType operator;

    @Column(nullable = false, length = 100)
    private String value;
}
