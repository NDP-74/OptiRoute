package com.optiroute.backend.repository.cost;

import com.optiroute.backend.entity.cost.CostRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostRuleRepository extends JpaRepository<CostRule, Long> {
    List<CostRule> findByCostParameterId(Long costParameterId);
}
