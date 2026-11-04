package com.optiroute.backend.repository.cost;

import org.springframework.data.jpa.repository.JpaRepository;
import com.optiroute.backend.entity.cost.CostCondition;

import java.util.List;

public interface CostConditionRepository extends JpaRepository<CostCondition, Long> {
    List<CostCondition> findByCostRuleId(Long costRuleId);

    void deleteByCostRuleId(Long costRuleId);
}