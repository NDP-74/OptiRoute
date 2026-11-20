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

export const normalizeVehicleRegistration = (registration: string): string =>
    registration.replace(/[^a-zA-Z0-9]/g, "").slice(0, 7).toUpperCase()

export const formatVehicleRegistration = (registration: string): string => {
    const compact = registration.replace(/-/g, "")
    if (!/^[a-zA-Z0-9]{7}$/.test(compact)) {
        return registration
    }

    return `${compact.slice(0, 2)}-${compact.slice(2, 5)}-${compact.slice(5, 7)}`
}

export const formatVehicleRegistrationInput = (registration: string): string => {
    const normalized = normalizeVehicleRegistration(registration)
    const firstGroup = normalized.slice(0, 2)
    const secondGroup = normalized.slice(2, 5)
    const thirdGroup = normalized.slice(5, 7)

    return `${firstGroup}${normalized.length >= 2 ? `-${secondGroup}` : ""}${normalized.length >= 5 ? `-${thirdGroup}` : ""}`
}