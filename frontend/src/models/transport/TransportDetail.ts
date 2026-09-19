import type { TransportStatus } from "@/types/planningType"

export interface TransportCostItem {
    label: string;
    amount: number;
}

export interface TransportCostCategory {
    costs: TransportCostItem[];
    totalCost: number;
}

export interface TransportCosts {
    vehicle: TransportCostCategory;
    driver: TransportCostCategory;
    structure: TransportCostCategory;
    totalCost: number;
}

export interface TransportDetail {
    id: number;
    name: string;
    status: TransportStatus;
    emptyTrip: boolean;

    plannedStart: string;
    plannedEnd: string;

    actualStart: string | null;
    actualEnd: string | null;

    driverId: number | null;
    driverName: string | null;
    driverEmail: string | null;

    tractorId: number | null;
    tractorRegistration: string | null;
    tractorBrand: string | null;
    tractorModel: string | null;

    semiTrailerId: number | null;
    semiTrailerRegistration: string | null;
    semiTrailerBrand: string | null;
    semiTrailerModel: string | null;

    customerId: number | null;
    customerName: string | null;
    customerAddress: string | null;
    customerCity: string | null;

    originName: string;
    originAddress: string | null;
    originLat: number;
    originLng: number;

    destinationName: string;
    destinationAddress: string | null;
    destinationLat: number;
    destinationLng: number;

    distanceMeters: number | null;
    durationSeconds: number | null;

    polyline: string | null;

    costs: TransportCosts;
    revenue: number;
}