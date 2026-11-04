package com.optiroute.backend.service.cost;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.repository.cost.CostParameterRepository;

import com.optiroute.backend.entity.cost.CostCondition;
import com.optiroute.backend.repository.cost.CostConditionRepository;
import com.optiroute.backend.dto.response.cost.CostParameterResponse;
import com.optiroute.backend.dto.response.cost.CostParameterLightResponse;
import com.optiroute.backend.dto.response.cost.CostConditionResponse;
import com.optiroute.backend.dto.request.cost.CreateCostParameterRequest;
import com.optiroute.backend.dto.request.cost.UpdateCostParameterRequest;
import com.optiroute.backend.dto.request.cost.CostConditionRequest;
import com.optiroute.backend.type.cost.CostParameterAssignmentType;

import java.util.List;

@Service
@Transactional
public class CostParameterService {

    private final CostParameterRepository costParameterRepository;
    private final CostConditionRepository costConditionRepository;

    public CostParameterService(CostParameterRepository costParameterRepository, CostConditionRepository costConditionRepository) {
        this.costParameterRepository = costParameterRepository;
        this.costConditionRepository = costConditionRepository;
    }

    public CostParameterResponse create(CreateCostParameterRequest request) {
        CostParameter parameter = new CostParameter();

        parameter.setCategory(request.category());
        parameter.setLabel(request.label());
        parameter.setValue(request.value());
        parameter.setUnit(request.unit());
        parameter.setActive(request.active());
        if (request.assignmentType() != null) {
            parameter.setAssignmentType(request.assignmentType());
        }

        parameter = costParameterRepository.save(parameter);

        replaceConditions(parameter, request.conditions());

        return getById(parameter.getId());
    }

    public CostParameterResponse update(Long id, UpdateCostParameterRequest request) {

        CostParameter parameter = costParameterRepository.findById(id).orElseThrow(() -> new RuntimeException("Cost parameter not found"));

        parameter.setCategory(request.category());
        parameter.setLabel(request.label());
        parameter.setValue(request.value());
        parameter.setUnit(request.unit());
        parameter.setActive(request.active());
        if (request.assignmentType() != null) {
            parameter.setAssignmentType(request.assignmentType());
        }

        costParameterRepository.save(parameter);

        replaceConditions(parameter, request.conditions());

        return getById(id);
    }

    @Transactional(readOnly = true)
    public CostParameterResponse getById(Long id) {

        CostParameter parameter = costParameterRepository.findById(id).orElseThrow(() -> new RuntimeException("Cost parameter not found"));

        return toResponse(parameter);
    }

    @Transactional(readOnly = true)
    public List<CostParameterLightResponse> getAll() {

        return costParameterRepository.findAll().stream().map(parameter -> new CostParameterLightResponse(parameter.getId(), parameter.getLabel(), parameter.getValue(),
            parameter.getUnit(), parameter.getCategory(), parameter.isActive(), parameter.getAssignmentType())).toList();
    }

    public void delete(Long id) {

        CostParameter parameter = costParameterRepository.findById(id).orElseThrow(() -> new RuntimeException("Cost parameter not found"));

        costConditionRepository.deleteByCostParameterId(parameter.getId());

        costParameterRepository.delete(parameter);
    }

    public CostParameterResponse setActive(Long id, boolean active) {

        CostParameter parameter = costParameterRepository.findById(id).orElseThrow(() -> new RuntimeException("Cost parameter not found"));

        parameter.setActive(active);

        costParameterRepository.save(parameter);

        return toResponse(parameter);
    }

    private void replaceConditions(CostParameter parameter, List<CostConditionRequest> requests) {
        costConditionRepository.deleteByCostParameterId(parameter.getId());
        if (parameter.getAssignmentType() == CostParameterAssignmentType.MANUAL
            || parameter.getAssignmentType() == CostParameterAssignmentType.ALWAYS || requests == null) {
            return;
        }

        for (CostConditionRequest conditionRequest : requests) {
            CostCondition condition = new CostCondition();

            condition.setCostParameter(parameter);
            condition.setSource(conditionRequest.source());
            condition.setField(conditionRequest.field());
            condition.setOperator(conditionRequest.operator());
            condition.setValue(conditionRequest.value());

            costConditionRepository.save(condition);
        }
    }

    private CostParameterResponse toResponse(CostParameter parameter) {
        List<CostConditionResponse> conditions = costConditionRepository.findByCostParameterId(parameter.getId()).stream()
            .map(condition -> new CostConditionResponse(condition.getId(), condition.getSource(), condition.getField(), condition.getOperator(), condition.getValue()))
            .toList();
        return new CostParameterResponse(parameter.getId(), parameter.getCategory(), parameter.getLabel(), parameter.getValue(), parameter.getUnit(), parameter.isActive(), parameter.getAssignmentType(),
            conditions);
    }
}