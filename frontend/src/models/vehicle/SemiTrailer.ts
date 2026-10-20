export interface SemiTrailerSummary {
    id: number

    registration: string
    depreciationStartDate: string | null
    depreciationEndDate: string | null
}

export interface SemiTrailerDetails extends SemiTrailerSummary {
    trailerType: string | null

    purchaseCost: number | null

    depreciationStartDate: string | null
    depreciationEndDate: string | null

    maintenanceCostPerKm: number | null
}

export interface CreateSemiTrailerRequest {
    registration: string

    trailerType: string | null

    purchaseCost: number | null

    depreciationStartDate: string | null
    depreciationEndDate: string | null

    maintenanceCostPerKm: number | null
}

export type UpdateSemiTrailerRequest = CreateSemiTrailerRequest

export interface SemiTrailerFormData {
    registration: string

    trailerType: string | null

    purchaseCost: number | null
    depreciationStartDate: string | null
    depreciationEndDate: string | null
    maintenanceCostPerKm: number | null
}
