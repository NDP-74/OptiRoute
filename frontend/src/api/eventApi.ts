import api from "./axios";

import type { EventCreateRequest, EventResponse } from "@/models/Event";

export const createEvent = async (event: EventCreateRequest): Promise<EventResponse> => {
    const response = await api.post<EventResponse>("/events", event);

    return response.data;
};
