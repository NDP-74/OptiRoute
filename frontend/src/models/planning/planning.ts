export interface PlanningTransport {
    id: number;
    name: string;

    emptyTrip: boolean;

    driverId: number;
    driverName: string;

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
    salaryForNonTransportDays: number;
}

export interface PlanningResponse {
    transports: PlanningTransport[];
    drivers: PlanningDriverSummary[];
}

export interface PlanningDriver {
    id: number;
    name: string;
    totalCost: number;
    days: Record<string, PlanningTransport[]>;
}

export interface PlanningDay {
    key: string;
    label: string;
    date: Date;
}