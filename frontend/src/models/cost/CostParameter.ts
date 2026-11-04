import type { CostConditionField, CostConditionOperator, CostParameterCategory, CostParameterUnit, CostConditionSource, CostParameterAssignmentType } from '@/types/CostParameterType'

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
    conditions: CostCondition[]
}

export interface CreateCostParameterRequest {
    category: CostParameterCategory
    label: string
    value: number | null
    unit: CostParameterUnit
    active: boolean
    assignmentType: CostParameterAssignmentType
    conditions: CostConditionRequest[]
}

export interface UpdateCostParameterRequest {
    category: CostParameterCategory
    label: string
    value: number | null
    unit: CostParameterUnit
    active: boolean
    assignmentType: CostParameterAssignmentType
    conditions: CostConditionRequest[]
}

export interface CostParameterFormData {
    category: CostParameterCategory
    label: string
    value: number | null
    unit: CostParameterUnit
    active: boolean
    assignmentType: CostParameterAssignmentType
    conditions: CostConditionRequest[]
}