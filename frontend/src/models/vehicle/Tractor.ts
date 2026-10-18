export interface TractorSummary {
    id: number

    registration: string
}

export interface TractorDetails extends TractorSummary {
    purchaseCost: number | null

    depreciationStartDate: string | null
    depreciationEndDate: string | null

    maintenanceCostPerKm: number | null
}

export interface CreateTractorRequest {
    registration: string

    purchaseCost: number | null

    depreciationStartDate: string | null
    depreciationEndDate: string | null

    maintenanceCostPerKm: number | null
}

export type UpdateTractorRequest = CreateTractorRequest

export interface TractorFormData {
    registration: string

    purchaseCost: number | null
    depreciationStartDate: string | null
    depreciationEndDate: string | null
    maintenanceCostPerKm: number | null
}
