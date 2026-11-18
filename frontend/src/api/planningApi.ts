import type { TransportDetail } from "@/models/transport/TransportDetail";
import api from "./axios";


import type { PlanningRequest, PlanningResponse } from "@/models/planning/planning";
import type { PlanningViewDraft, SavedPlanningView } from "@/models/planning/PlanningView";

export const getPlanningViews = async (): Promise<SavedPlanningView[]> =>
    (await api.get<SavedPlanningView[]>("/planning-views")).data;

export const savePlanningView = async (draft: PlanningViewDraft, id?: number): Promise<SavedPlanningView> =>
    (id === undefined
        ? await api.post<SavedPlanningView>("/planning-views", draft)
        : await api.put<SavedPlanningView>(`/planning-views/${id}`, draft)).data;

export const deletePlanningView = async (id: number): Promise<void> => {
    await api.delete(`/planning-views/${id}`);
};

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