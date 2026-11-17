package com.optiroute.backend.repository.cost;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.type.cost.CostParameterCategoryType;

import java.util.Optional;
import java.util.List;
import com.optiroute.backend.type.cost.CostParameterCategoryType;

public interface CostParameterRepository extends JpaRepository<CostParameter, Long> {
    Optional<CostParameter> findByCategoryAndLabel(CostParameterCategoryType category, String label);

    List<CostParameter> findByCategory(CostParameterCategoryType category);

    List<CostParameter> findByCategoryIn(List<CostParameterCategoryType> categories);
}