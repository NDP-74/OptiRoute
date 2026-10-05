import api from "./axios";

import type { PointOfInterest, PointOfInterestCreateRequest } from "@/models/PointOfInterest";

export const getPointsOfInterest = async (): Promise<PointOfInterest[]> => {
    const response = await api.get<PointOfInterest[]>("/points-of-interest");

    return response.data;
};

export const createPointOfInterest = async (point: PointOfInterestCreateRequest): Promise<PointOfInterest> => {
    const response = await api.post<PointOfInterest>("/points-of-interest", point);

    return response.data;
};
