import type { FuelType } from "@/types/vehicleType"
import type { SemiTrailerFormData } from "@/models/vehicle/SemiTrailer"
import type { TractorFormData } from "@/models/vehicle/Tractor"

const FUEL_TYPE_LABELS: Record<FuelType, string> = {
    DIESEL: "Diesel",
    B100: "B100",
    HVO100: "HVO100",
    ELECTRIC: "Électrique",
    LNG: "GNL",
    CNG: "GNC",
    HYBRID: "Hybride",
    OTHER: "Autre",
}

export const formatFuelType = (fuelType: FuelType | null | undefined,): string => {
    if (!fuelType) {
        return "Non renseigné"
    }

    return FUEL_TYPE_LABELS[fuelType]
}

export const formatTrailerType = (trailerType: string | null | undefined,): string => {
    if (!trailerType) {
        return "Non renseigné"
    }

    return trailerType
}

export const formatVehicleLabel = (brand: string | null | undefined, model: string | null | undefined, emptyLabel = "Marque et modèle non renseignés",): string => {
    const parts = [brand, model].filter(
        (value): value is string => Boolean(value),
    )

    return parts.length > 0 ? parts.join(" ") : emptyLabel
}

export const formatConsumption = (value: number | null): string => {
    if (value === null) {
        return "Non renseignée"
    }

    return `${value.toLocaleString("fr-FR", { maximumFractionDigits: 2 })} L / 100 km`
}

export const createEmptySemiTrailerForm = (): SemiTrailerFormData => ({
    registration: "",
    brand: null,
    model: null,

    maxSpeed: null,

    trailerType: null,

    emptyWeightKg: null,
    grossVehicleWeightKg: null,

    heightCm: null,
    widthCm: null,
    lengthCm: null,

    axleCount: null,

    purchaseCost: null,
    depreciationStartDate: null,
    depreciationEndDate: null,
    maintenanceCostPerKm: null,
})

export const createEmptyTractorForm = (): TractorFormData => ({
    registration: "",
    brand: null,
    model: null,

    maxSpeed: null,

    fuelType: "DIESEL",
    averageConsumption: null,
    averageConsumptionEmpty: null,

    emptyWeightKg: null,
    grossCombinationWeightKg: null,

    heightCm: null,
    widthCm: null,
    lengthCm: null,

    axleCount: null,

    purchaseCost: null,
    depreciationStartDate: null,
    depreciationEndDate: null,
    maintenanceCostPerKm: null,
})