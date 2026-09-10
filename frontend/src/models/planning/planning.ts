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
    days: Record<string, PlanningTransport[]>;
}

export interface PlanningDay {
    key: string;
    label: string;
    date: Date;
}