import type { CostConditionField, CostConditionOperator, CostRuleLogicalOperator, CostParameterCategory, CostParameterUnit, CostConditionSource, CostParameterAssignmentType } from '@/types/CostParameterType'

/* CostCondition */

export interface CostCondition {
    id: number
    source: CostConditionSource
    field: CostConditionField
    operator: CostConditionOperator
    value: string
}

export interface CostConditionRequest {
    source: CostConditionSource
    field: CostConditionField
    operator: CostConditionOperator
    value: string
}

/* CostRule */

export interface CostRule {
    id: number
    logicalOperator: CostRuleLogicalOperator
    conditions: CostCondition[]
}

export interface CostRuleRequest {
    logicalOperator: CostRuleLogicalOperator
    conditions: CostConditionRequest[]
}

/* CostParameter */

export interface CostParameterLight {
    id: number
    assignmentType: CostParameterAssignmentType
    label: string
    value: number
    unit: CostParameterUnit
    category: CostParameterCategory
    active: boolean
}

export interface CostParameter {
    id: number
    category: CostParameterCategory
    label: string
    value: number
    unit: CostParameterUnit
    active: boolean
    assignmentType: CostParameterAssignmentType
    rule: CostRule | null
}

export interface CreateCostParameterRequest {
    category: CostParameterCategory
    label: string
    value: number | null
    unit: CostParameterUnit
    active: boolean
    assignmentType: CostParameterAssignmentType
    rule: CostRuleRequest | null
}

export interface UpdateCostParameterRequest {
    category: CostParameterCategory
    label: string
    value: number | null
    unit: CostParameterUnit
    active: boolean
    assignmentType: CostParameterAssignmentType
    rule: CostRuleRequest | null
}

export interface CostParameterFormData {
    category: CostParameterCategory
    label: string
    value: number | null
    unit: CostParameterUnit
    active: boolean
    assignmentType: CostParameterAssignmentType
    rule: CostRuleRequest | null
}