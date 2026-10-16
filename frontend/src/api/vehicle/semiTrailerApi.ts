import api from "../axios";

import type {
    SemiTrailerSummary,
    SemiTrailerDetails,
    CreateSemiTrailerRequest,
    UpdateSemiTrailerRequest,
} from "@/models/vehicle/SemiTrailer";

export type TrailerTypeOption = {
    id: number;
    label: string;
};

export type TrailerTypeRequest = {
    label: string;
};

export const getSemiTrailers = async (): Promise<SemiTrailerSummary[]> => {
    const response = await api.get<SemiTrailerSummary[]>("/semiTrailers");

    return response.data;
};

export const getSemiTrailer = async (id: number): Promise<SemiTrailerDetails> => {
    const response = await api.get<SemiTrailerDetails>(`/semiTrailers/${id}`);

    return response.data;
};

export const getSemiTrailerTypes = async (): Promise<TrailerTypeOption[]> => {
    const response = await api.get<TrailerTypeOption[]>("/semiTrailers/types");

    return response.data;
};

export const getTrailerType = async (id: number): Promise<TrailerTypeOption> => {
    const response = await api.get<TrailerTypeOption>(`/semiTrailers/types/${id}`);

    return response.data;
};

export const createTrailerType = async (trailerType: TrailerTypeRequest): Promise<TrailerTypeOption> => {
    const response = await api.post<TrailerTypeOption>("/semiTrailers/types", trailerType);

    return response.data;
};

export const updateTrailerType = async (id: number, trailerType: TrailerTypeRequest): Promise<TrailerTypeOption> => {
    const response = await api.put<TrailerTypeOption>(`/semiTrailers/types/${id}`, trailerType);

    return response.data;
};

export const deleteTrailerType = async (id: number): Promise<void> => {
    await api.delete(`/semiTrailers/types/${id}`);
};

export const createSemiTrailer = async (semiTrailer: CreateSemiTrailerRequest): Promise<SemiTrailerSummary> => {
    const response = await api.post<SemiTrailerSummary>("/semiTrailers", semiTrailer);

    return response.data;
};

export const updateSemiTrailer = async (id: number, semiTrailer: UpdateSemiTrailerRequest): Promise<SemiTrailerSummary> => {
    const response = await api.put<SemiTrailerSummary>(`/semiTrailers/${id}`, semiTrailer);

    return response.data;
};

export const deleteSemiTrailer = async (id: number): Promise<void> => {
    await api.delete(`/semiTrailers/${id}`);
};