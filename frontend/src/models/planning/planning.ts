import type { DriverType } from "@/models/driver/Driver";

export interface PlanningTransport {
    id: number;
    routeId: number;
    routeNumber: number;
    routeCount: number;
    name: string;

    emptyTrip: boolean;

    driverId: number;
    driverName: string;

    tractorRegistration: string | null;
    semiTrailerRegistration: string | null;

    departureStart: string | null;
    departureEnd: string | null;
    arrivalStart: string | null;
    arrivalEnd: string | null;

    originName: string;
    destinationName: string;

    totalCost: number;
    driverCost: number;
    structureCost: number;
    vehicleCost: number;
    revenue: number;
}

export interface PlanningRequest {
    startDate: string;
    endDate: string;
}

export interface PlanningDriverSummary {
    id: number;
    name: string;
    tractorRegistration: string | null;
    semiTrailerRegistration: string | null;
    salaryForNonTransportDays: number;
    costType: "FIXED" | "HOURLY" | null;
    driverType: DriverType;
}

export interface PlanningUnassignedVehicles {
    registrations: string[];
    depreciationCost: number;
    vehicles: PlanningUnassignedVehicle[];
}

export interface PlanningUnassignedVehicle {
    registration: string;
    depreciationCost: number;
}

export interface PlanningResponse {
    transports: PlanningTransport[];
    drivers: PlanningDriverSummary[];
    unassignedVehicles: PlanningUnassignedVehicles;
}

export interface PlanningDriver {
    id: number;
    name: string;
    vehicleRegistrations: string[];
    totalCost: number;
    totalRevenue: number;
    driverCost: number;
    structureCost: number;
    vehicleCost: number;
    vehicleDepreciationCosts?: PlanningUnassignedVehicle[];
    days: Record<string, PlanningTransport[]>;
}

export interface PlanningDay {
    key: string;
    label: string;
    date: Date;
}