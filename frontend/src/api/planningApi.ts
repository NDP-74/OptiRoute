import type { TransportDetail } from "@/models/transport/TransportDetail";
import api from "./axios";


import type { PlanningRequest, PlanningResponse } from "@/models/planning/planning";

export const getPlanning = async (request: PlanningRequest): Promise<PlanningResponse> => {
    const response = await api.get<PlanningResponse>("/transports/planning",
        {
            params: request,
        }
    );

    return response.data;
};

export const getTransportById = async (serviceId: number, routeId?: number): Promise<TransportDetail> => {
    const url = routeId === undefined ? `/transports/${serviceId}` : `/transports/${serviceId}/routes/${routeId}`;
    const response = await api.get<TransportDetail>(url);

    return response.data;
};