import type { CostParameterFormData } from "@/models/cost/CostParameter"
import type { CostConditionField, CostConditionOperator, CostConditionSource, CostParameterAssignmentType, CostParameterCategory, CostParameterUnit } from "@/types/CostParameterType"

export const createEmptyCostParameterForm = (): CostParameterFormData => ({
    category: "VEHICLE",
    label: "",
    value: 0,
    unit: "EUR_PER_TRIP",
    active: true,
    assignmentType: "AUTOMATIC",
    rule: null,
})

export const assignmentTypeLabels: Record<CostParameterAssignmentType, string> = {
    AUTOMATIC: "Automatique",
    MANUAL: "Manuelle",
}

export type CostOption<T extends string> = {
    value: T
    label: string
}

export const categoryOptions: CostOption<CostParameterCategory>[] = [
    { value: "VEHICLE", label: "Véhicule" },
    { value: "DRIVER", label: "Conducteur" },
    { value: "STRUCTURE", label: "Structure" },
]

export const unitOptions: CostOption<CostParameterUnit>[] = [
    { value: "EUR_PER_KM", label: "€/km" },
    { value: "EUR_PER_TRIP", label: "€/trajet" },
    { value: "EUR_PER_HOUR", label: "€/heure" },
    { value: "EUR_PER_DAY", label: "€/jour" },
    { value: "EUR_PER_MONTH", label: "€/mois" },
    { value: "EUR_PER_YEAR", label: "€/an" },
]

export const sourceOptions: CostOption<CostConditionSource>[] = [
    { value: "TRIP", label: "Trajet" },
    { value: "VEHICLE", label: "Véhicule" },
    { value: "DRIVER", label: "Conducteur" },
]

export const tripFieldOptions: CostOption<CostConditionField>[] = [
    { value: "DISTANCE", label: "Distance" },
    { value: "DURATION", label: "Durée" },
    { value: "DEPARTURE_TIME", label: "Heure de départ" },
    { value: "ARRIVAL_TIME", label: "Heure d'arrivée" },
    { value: "EMPTY_TRIP", label: "Trajet à vide" },
    { value: "LOADED_TRIP", label: "Trajet chargé" },
]

export const vehicleFieldOptions: CostOption<CostConditionField>[] = [
    { value: "VEHICLE_TYPE", label: "Type de semi-remorque" },
]

export const driverFieldOptions: CostOption<CostConditionField>[] = [
    { value: "DRIVER_DAY_START_TIME", label: "Heure de début de journée" },
    { value: "DRIVER_DAY_END_TIME", label: "Heure de fin de journée" },
]

export const equalityOperatorOptions: CostOption<CostConditionOperator>[] = [
    { value: "EQUALS", label: "est égal à" },
    { value: "NOT_EQUALS", label: "est différent de" },
]

export const numericOperatorOptions: CostOption<CostConditionOperator>[] = [
    { value: "LESS_THAN", label: "est inférieur à" },
    { value: "GREATER_THAN", label: "est supérieur à" },
]

export const timeOperatorOptions: CostOption<CostConditionOperator>[] = [
    { value: "BEFORE", label: "avant" },
    { value: "AFTER", label: "après" },
]

export const categoryLabels: Record<CostParameterCategory, string> = Object.fromEntries(
    categoryOptions.map((option) => [option.value, option.label]),
) as Record<CostParameterCategory, string>

export const unitLabels: Record<CostParameterUnit, string> = Object.fromEntries(
    unitOptions.map((option) => [option.value, option.label]),
) as Record<CostParameterUnit, string>

export const formatUnit = (unit: CostParameterUnit | string) => {
    return unitLabels[unit as CostParameterUnit] ?? unit
}

export const conditionSourceLabels: Record<CostConditionSource, string> = Object.fromEntries(
    sourceOptions.map((option) => [option.value, option.label]),
) as Record<CostConditionSource, string>

export const conditionFieldLabels: Record<CostConditionField, string> = Object.fromEntries(
    [...tripFieldOptions, ...vehicleFieldOptions, ...driverFieldOptions]
        .map((option) => [option.value, option.label]),
) as Record<CostConditionField, string>

export const conditionOperatorLabels: Record<CostConditionOperator, string> = Object.fromEntries(
    [...equalityOperatorOptions, ...numericOperatorOptions, ...timeOperatorOptions]
        .map((option) => [option.value, option.label]),
) as Record<CostConditionOperator, string>

export const conditionValueUnits: Partial<Record<CostConditionField, string>> = {
    DISTANCE: "km",
    DURATION: "heures",
}

export const getFieldOptions = (source: CostConditionSource) => {
    switch (source) {
        case "TRIP":
            return tripFieldOptions
        case "VEHICLE":
            return vehicleFieldOptions
        case "DRIVER":
            return driverFieldOptions
    }
}

export const getOperatorOptions = (field: CostConditionField) => {
    if (["DEPARTURE_TIME", "ARRIVAL_TIME", "DRIVER_DAY_START_TIME", "DRIVER_DAY_END_TIME"].includes(field)) {
        return timeOperatorOptions
    }

    if (["DISTANCE", "DURATION"].includes(field)) {
        return numericOperatorOptions
    }

    return equalityOperatorOptions
}

export const formatConditionValue = (field: CostConditionField, value: string, trailerTypeLabels: Record<string, string> = {}) => {
    if (["EMPTY_TRIP", "LOADED_TRIP"].includes(field)) {
        return value === "true" ? "Oui" : value === "false" ? "Non" : value
    }

    if (field === "VEHICLE_TYPE") {
        return trailerTypeLabels[value] ?? "Type inconnu"
    }

    const unit = conditionValueUnits[field]
    return unit ? `${value} ${unit}` : value
}

export const formatValue = (value: number, unit: string) => {
    return `${value} ${formatUnit(unit)}`
}
