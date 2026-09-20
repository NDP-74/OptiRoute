export interface CustomerSummary {
    id: number;
    name: string;
    code: string | null;
    city: string | null;
}

export interface CustomerDetails {
    id: number;

    externalId: string | null;
    externalSource: string;

    name: string;
    code: string | null;

    address: string | null;
    city: string | null;
    country: string | null;

    createdAt: string;
    updatedAt: string;
}

export interface CustomerFormData {
    name: string;
    code: string | null;
    address: string | null;
    city: string | null;
    country: string | null;
}

export interface CustomerUpdateRequest extends CustomerFormData {
    externalId: string | null;
    externalSource: string;
}

export interface CustomerCreateRequest extends CustomerFormData {
    externalId: string | null;
    externalSource: string;
}