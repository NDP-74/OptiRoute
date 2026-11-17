package com.optiroute.backend.repository.cost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.optiroute.backend.entity.cost.CostCondition;

import java.util.List;

public interface CostConditionRepository extends JpaRepository<CostCondition, Long> {
    List<CostCondition> findByCostParameterId(Long costParameterId);

    @EntityGraph(attributePaths = "costParameter")
    List<CostCondition> findByCostParameterIdIn(List<Long> costParameterIds);

    void deleteByCostParameterId(Long costParameterId);
}