import type { RouteDto } from '@/models/route/Route'
import type { Position } from '@/models/route/Position'

export interface TimeWindows {
    departureStart?: string | null
    departureEnd?: string | null
    arrivalStart?: string | null
    arrivalEnd?: string | null
}

export interface ServiceTiming extends TimeWindows {
    loadingDurationMinutes?: number | null
    unloadingDurationMinutes?: number | null
}

export interface CreateTransportRequest {
    name: string
    customerId?: number

    driverId: number
    tractorId: number
    semiTrailerId?: number

    emptyTrip: boolean

    routeTime?: string | null
    timeMode?: 'DEPARTURE' | 'ARRIVAL'

    originName: string
    originAddress: string
    originLat: number
    originLng: number

    destinationName: string
    destinationAddress: string
    destinationLat: number
    destinationLng: number

    revenue?: number | null
    serviceTiming?: ServiceTiming
    waypoints?: Position[]
}

export interface CreateTransportFromRouteRequest {
    transport: CreateTransportRequest
    selectedRoute: RouteDto
}

export interface AssignTransport {
    title: string
    driverId: number
    customerId?: number
}