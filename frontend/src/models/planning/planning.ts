export interface PlanningTransport {
    id: number;
    name: string;

    emptyTrip: boolean;

    driverId: number;
    driverName: string;

    tractorRegistration: string | null;
    semiTrailerRegistration: string | null;

    plannedStart: string;
    plannedEnd: string;

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
}

export interface PlanningUnassignedVehicles {
    registrations: string[];
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
    eventCost: number;
    days: Record<string, PlanningTransport[]>;
    events: Record<string, import("@/models/vehicle/VehicleEvent").VehicleEventResponse[]>;
}

export interface PlanningDay {
    key: string;
    label: string;
    date: Date;
}