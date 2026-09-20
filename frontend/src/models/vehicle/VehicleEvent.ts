export interface VehicleEventResponse {
    id: number
    eventDate: string
    supplier: string | null
    cost: number
    tractorId: number | null
    tractorRegistration: string | null
    semiTrailerId: number | null
    semiTrailerRegistration: string | null
}

export interface VehicleEventRequest {
    eventDate: string
    supplier: string | null
    cost: number | null
    tractorId: number | null
    semiTrailerId: number | null
}