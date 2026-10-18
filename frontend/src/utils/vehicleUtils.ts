import type { SemiTrailerFormData } from "@/models/vehicle/SemiTrailer"
import type { TractorFormData } from "@/models/vehicle/Tractor"

export const createEmptySemiTrailerForm = (): SemiTrailerFormData => ({
    registration: "",

    trailerType: null,

    purchaseCost: null,
    depreciationStartDate: null,
    depreciationEndDate: null,
    maintenanceCostPerKm: null,
})

export const createEmptyTractorForm = (): TractorFormData => ({
    registration: "",

    purchaseCost: null,
    depreciationStartDate: null,
    depreciationEndDate: null,
    maintenanceCostPerKm: null,
})