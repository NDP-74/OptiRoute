import type { FuelType } from "@/types/vehicleType"

export interface TractorSummary {
    id: number

    registration: string

    brand: string | null
    model: string | null
}

export interface TractorDetails extends TractorSummary {
    maxSpeed: number | null

    fuelType: FuelType | null
    averageConsumption: number | null
    averageConsumptionEmpty: number | null

    emptyWeightKg: number | null
    grossCombinationWeightKg: number | null

    heightCm: number | null
    widthCm: number | null
    lengthCm: number | null

    axleCount: number | null

    purchaseCost: number | null

    depreciationStartDate: string | null
    depreciationEndDate: string | null

    maintenanceCostPerKm: number | null

    externalId: string | null
    externalSource: string

    createdAt: string
    updatedAt: string
}

export interface CreateTractorRequest {
    externalId: string | null
    externalSource: string

    registration: string

    brand: string | null
    model: string | null

    maxSpeed: number | null

    fuelType: FuelType | null
    averageConsumption: number | null
    averageConsumptionEmpty: number | null

    emptyWeightKg: number | null
    grossCombinationWeightKg: number | null

    heightCm: number | null
    widthCm: number | null
    lengthCm: number | null

    axleCount: number | null

    purchaseCost: number | null

    depreciationStartDate: string | null
    depreciationEndDate: string | null

    maintenanceCostPerKm: number | null
}

export interface UpdateTractorRequest {
    registration: string

    brand: string | null
    model: string | null

    maxSpeed: number | null

    fuelType: FuelType | null
    averageConsumption: number | null
    averageConsumptionEmpty: number | null

    emptyWeightKg: number | null
    grossCombinationWeightKg: number | null

    heightCm: number | null
    widthCm: number | null
    lengthCm: number | null

    axleCount: number | null

    purchaseCost: number | null

    depreciationStartDate: string | null
    depreciationEndDate: string | null

    maintenanceCostPerKm: number | null
}

export interface TractorFormData {
    registration: string
    brand: string | null
    model: string | null

    maxSpeed: number | null

    fuelType: FuelType | null
    averageConsumption: number | null
    averageConsumptionEmpty: number | null

    emptyWeightKg: number | null
    grossCombinationWeightKg: number | null

    heightCm: number | null
    widthCm: number | null
    lengthCm: number | null

    axleCount: number | null

    purchaseCost: number | null
    depreciationStartDate: string | null
    depreciationEndDate: string | null
    maintenanceCostPerKm: number | null
}