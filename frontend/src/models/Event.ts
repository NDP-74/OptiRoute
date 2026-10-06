export interface EventCreateRequest {
    serviceId: number
    costParameterId: number
    pointOfInterestId: number | null
    tractorId: number | null
    semiTrailerId: number | null
}

export interface EventResponse {
    id: number
    routeId: number
    costParameterId: number
    costParameterLabel: string
    pointOfInterestId: number | null
    pointOfInterestLabel: string | null
    tractorId: number | null
    semiTrailerId: number | null
    eventDate: string
}
