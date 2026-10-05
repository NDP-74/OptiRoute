export interface PointOfInterestCreateRequest {
    label: string
    address: string | null
    latitude: number
    longitude: number
}

export interface PointOfInterest extends PointOfInterestCreateRequest {
    id: number
    createdAt: string
    updatedAt: string
}
