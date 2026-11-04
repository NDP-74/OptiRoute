package com.optiroute.backend.service.cost;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.repository.cost.CostParameterRepository;

import com.optiroute.backend.entity.cost.CostRule;
import com.optiroute.backend.repository.cost.CostRuleRepository;
import com.optiroute.backend.entity.cost.CostCondition;
import com.optiroute.backend.repository.cost.CostConditionRepository;
import com.optiroute.backend.dto.response.cost.CostParameterResponse;
import com.optiroute.backend.dto.response.cost.CostParameterLightResponse;
import com.optiroute.backend.dto.response.cost.CostRuleResponse;
import com.optiroute.backend.dto.response.cost.CostConditionResponse;
import com.optiroute.backend.dto.request.cost.CreateCostParameterRequest;
import com.optiroute.backend.dto.request.cost.UpdateCostParameterRequest;
import com.optiroute.backend.dto.request.cost.CostRuleRequest;
import com.optiroute.backend.dto.request.cost.CostConditionRequest;

import java.util.List;

@Service
@Transactional
public class CostParameterService {

    private final CostParameterRepository costParameterRepository;
    private final CostRuleRepository costRuleRepository;
    private final CostConditionRepository costConditionRepository;

    public CostParameterService(CostParameterRepository costParameterRepository, CostRuleRepository costRuleRepository, CostConditionRepository costConditionRepository) {
        this.costParameterRepository = costParameterRepository;
        this.costRuleRepository = costRuleRepository;
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

        if (request.rule() != null) {
            createRule(parameter,request.rule());
        }

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

        /*
         * PUT = remplacement complet de la règle.
         *
         * On supprime toujours l'ancienne règle,
         * puis on recrée celle envoyée par le frontend.
         */
        deleteRule(parameter);

        if (request.rule() != null) {
            createRule(parameter,request.rule());
        }

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

        deleteRule(parameter);

        costParameterRepository.delete(parameter);
    }

    public CostParameterResponse setActive(Long id, boolean active) {

        CostParameter parameter = costParameterRepository.findById(id).orElseThrow(() -> new RuntimeException("Cost parameter not found"));

        parameter.setActive(active);

        costParameterRepository.save(parameter);

        return toResponse(parameter);
    }

    private void createRule(CostParameter parameter, CostRuleRequest request) {

        CostRule rule = new CostRule();

        rule.setCostParameterId(parameter.getId());
        rule.setLogicalOperator(request.logicalOperator());

        rule = costRuleRepository.save(rule);

        for (CostConditionRequest conditionRequest : request.conditions()) {

            CostCondition condition = new CostCondition();

            condition.setCostRuleId(rule.getId());
            condition.setSource(conditionRequest.source());
            condition.setField(conditionRequest.field());
            condition.setOperator(conditionRequest.operator());
            condition.setValue(conditionRequest.value());

            costConditionRepository.save(condition);
        }
    }

    private void deleteRule(CostParameter parameter) {

        List<CostRule> rules = costRuleRepository.findByCostParameterId(parameter.getId());

        for (CostRule rule : rules) {

            costConditionRepository.deleteByCostRuleId(rule.getId());

            costRuleRepository.delete(rule);
        }
    }

    private CostParameterResponse toResponse(CostParameter parameter) {

        CostRuleResponse ruleResponse = null;

        List<CostRule> rules = costRuleRepository.findByCostParameterId(parameter.getId());

        if (!rules.isEmpty()) {

            CostRule rule = rules.get(0);

            List<CostConditionResponse> conditions = costConditionRepository.findByCostRuleId(rule.getId()).stream()
                .map(condition -> new CostConditionResponse(condition.getId(), condition.getSource(), condition.getField(), condition.getOperator(), condition.getValue()))
                .toList();

            ruleResponse = new CostRuleResponse(rule.getId(), rule.getLogicalOperator(), conditions);
        }

        return new CostParameterResponse(parameter.getId(), parameter.getCategory(), parameter.getLabel(), parameter.getValue(), parameter.getUnit(), parameter.isActive(), parameter.getAssignmentType(),
            ruleResponse);
    }
}