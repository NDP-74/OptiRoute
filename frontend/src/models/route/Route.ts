import type { Position } from '@/models/route/Position'

export interface RouteRequest {
    origin: Position
    destination: Position
    waypoints?: Position[]

    tractorId?: number
    semiTrailerId?: number

    mode: 'FASTEST' | 'CHEAPEST'
    emptyTrip: boolean

    routeTime?: string
    timeMode?: 'DEPARTURE' | 'ARRIVAL'

    driverHourlyRate: number
}

export interface RouteResponse {
    routes: RouteDto[]
}

export interface RouteDto {
    duration: number
    baseDuration: number
    distanceMeters: number
    polyline?: string

    costs: RouteCostDetailsDto
}

export interface RouteCostDetailsDto {
    fuelCost: number
    tollCost: number
    totalCost: number
}