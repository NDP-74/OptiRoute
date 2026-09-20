import api from "./axios";

import type { CustomerCreateRequest, CustomerDetails, CustomerSummary, CustomerUpdateRequest } from "@/models/Customer";

export const getCustomers = async (): Promise<CustomerSummary[]> => {
    const response = await api.get<CustomerSummary[]>("/customers");

    return response.data;
};

export const getCustomer = async (id: number): Promise<CustomerDetails> => {
    const response = await api.get<CustomerDetails>(`/customers/${id}`);

    return response.data;
};

export const createCustomer = async (customer: CustomerCreateRequest): Promise<CustomerDetails> => {
    const response = await api.post<CustomerDetails>("/customers", customer);

    return response.data;
};

export const updateCustomer = async (id: number, customer: CustomerUpdateRequest): Promise<CustomerDetails> => {
    const response = await api.put<CustomerDetails>(`/customers/${id}`, customer);

    return response.data;
};

export const deleteCustomer = async (id: number): Promise<void> => {
    await api.delete(`/customers/${id}`);
};