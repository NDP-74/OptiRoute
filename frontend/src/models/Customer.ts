export interface CustomerSummary {
    id: number;
    name: string;
    code: string | null;
}

export interface CustomerDetails {
    id: number;
    name: string;
    code: string | null;

    address: string | null;
}

export interface CustomerFormData {
    name: string;
    code: string | null;
    address: string | null;
}

export type CustomerUpdateRequest = CustomerFormData;

export type CustomerCreateRequest = CustomerFormData;