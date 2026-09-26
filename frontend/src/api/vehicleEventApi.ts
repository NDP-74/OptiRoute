import api from "@/api/axios"

import type { VehicleEventRequest, VehicleEventResponse } from "@/models/vehicle/VehicleEvent"

export const getVehicleEventsByDateRange = async (startDate: string, endDate: string,): Promise<VehicleEventResponse[]> => {
    const response = await api.get<VehicleEventResponse[]>("/vehicle-events/by-range", {
        params: { startDate, endDate },
    })

    return response.data
}

export const getVehicleEventsByTractor = async (tractorId: number): Promise<VehicleEventResponse[]> => {
    const response = await api.get<VehicleEventResponse[]>(`/vehicle-events/by-tractor/${tractorId}`)

    return response.data
}

export const getVehicleEventsBySemiTrailer = async (semiTrailerId: number): Promise<VehicleEventResponse[]> => {
    const response = await api.get<VehicleEventResponse[]>(`/vehicle-events/by-semi-trailer/${semiTrailerId}`)

    return response.data
}

export const getVehicleEvent = async (id: number): Promise<VehicleEventResponse> => {
    const response = await api.get<VehicleEventResponse>(`/vehicle-events/${id}`)

    return response.data
}

export const createVehicleEvent = async (event: VehicleEventRequest): Promise<VehicleEventResponse> => {
    const response = await api.post<VehicleEventResponse>("/vehicle-events", event)

    return response.data
}

export const updateVehicleEvent = async (id: number, event: VehicleEventRequest): Promise<VehicleEventResponse> => {
    const response = await api.put<VehicleEventResponse>(`/vehicle-events/${id}`, event)

    return response.data
}

export const deleteVehicleEvent = async (id: number): Promise<void> => {
    await api.delete(`/vehicle-events/${id}`)
}