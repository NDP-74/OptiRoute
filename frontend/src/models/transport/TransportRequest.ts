import type { RouteDto } from '@/models/route/Route'

export interface CreateTransportRequest {
    name: string
    customerId?: number

    driverId: number
    tractorId: number
    semiTrailerId?: number

    emptyTrip: boolean

    plannedStart: string
    plannedEnd: string

    originName: string
    originAddress: string
    originLat: number
    originLng: number

    destinationName: string
    destinationAddress: string
    destinationLat: number
    destinationLng: number

    revenue?: number | null
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