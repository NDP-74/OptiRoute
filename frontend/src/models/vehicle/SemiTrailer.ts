export interface SemiTrailerSummary {
    id: number

    registration: string

    brand: string | null
    model: string | null
}

export interface SemiTrailerDetails extends SemiTrailerSummary {
    trailerType: string | null

    maxSpeed: number | null

    emptyWeightKg: number | null
    grossVehicleWeightKg: number | null

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

export interface CreateSemiTrailerRequest {
    externalId: string | null
    externalSource: string

    registration: string

    brand: string | null
    model: string | null

    trailerType: string | null

    maxSpeed: number | null

    emptyWeightKg: number | null
    grossVehicleWeightKg: number | null

    heightCm: number | null
    widthCm: number | null
    lengthCm: number | null

    axleCount: number | null

    purchaseCost: number | null

    depreciationStartDate: string | null
    depreciationEndDate: string | null

    maintenanceCostPerKm: number | null
}

export interface UpdateSemiTrailerRequest {
    registration: string

    brand: string | null
    model: string | null

    trailerType: string | null

    maxSpeed: number | null

    emptyWeightKg: number | null
    grossVehicleWeightKg: number | null

    heightCm: number | null
    widthCm: number | null
    lengthCm: number | null

    axleCount: number | null

    purchaseCost: number | null

    depreciationStartDate: string | null
    depreciationEndDate: string | null

    maintenanceCostPerKm: number | null
}

export interface SemiTrailerFormData {
    registration: string
    brand: string | null
    model: string | null

    maxSpeed: number | null

    trailerType: string | null

    emptyWeightKg: number | null
    grossVehicleWeightKg: number | null

    heightCm: number | null
    widthCm: number | null
    lengthCm: number | null

    axleCount: number | null

    purchaseCost: number | null
    depreciationStartDate: string | null
    depreciationEndDate: string | null
    maintenanceCostPerKm: number | null
}