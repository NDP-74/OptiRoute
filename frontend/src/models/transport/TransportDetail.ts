import type { TransportStatus } from "@/types/planningType"
import type { ServiceTiming, TimeWindows } from "./TransportRequest"
import type { Position } from "@/models/route/Position"

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

export interface TransportRouteDetail {
    routeId: number;
    routeNumber: number;
    routeCount: number;
    emptyTrip: boolean;
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
    routeType: "FASTEST" | "CHEAPEST" | null;
    departureStart: string | null;
    departureEnd: string | null;
    arrivalStart: string | null;
    arrivalEnd: string | null;
    waypoints: Position[];
    costs: TransportCosts;
    revenue: number | null;
}

export interface TransportDetail extends TimeWindows {
    id: number;
    routeId: number;
    routeNumber: number;
    routeCount: number;
    name: string;
    status: TransportStatus;
    emptyTrip: boolean;

    driverId: number | null;
    driverName: string | null;
    driverEmail: string | null;

    tractorId: number | null;
    tractorRegistration: string | null;

    semiTrailerId: number | null;
    semiTrailerRegistration: string | null;

    customerId: number | null;
    customerName: string | null;
    customerAddress: string | null;

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
    routeType: "FASTEST" | "CHEAPEST" | null;

    costs: TransportCosts;
    revenue: number;
    serviceTiming: ServiceTiming;
    waypoints: Position[];
    routes: TransportRouteDetail[];
}