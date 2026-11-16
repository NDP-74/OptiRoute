import { ref } from "vue";

import { getPlanning } from "@/api/planningApi";

import { formatParisDateKey } from "@/utils/formatters";

import type { PlanningRequest, PlanningResponse, PlanningTransport } from "@/models/planning/planning";

export function usePlanning() {

    const loading = ref(false);
    const transports = ref<PlanningResponse["transports"]>([]);
    const drivers = ref<PlanningResponse["drivers"]>([]);
    const unassignedVehicles = ref<PlanningResponse["unassignedVehicles"]>({ registrations: [], depreciationCost: 0, vehicles: [] });
    const error = ref<string | null>(null);

    async function loadPlanning(request: PlanningRequest) {
        try {
            loading.value = true;
            error.value = null;
            const planning = await getPlanning(request);
            transports.value = planning.transports;
            drivers.value = planning.drivers;
            unassignedVehicles.value = planning.unassignedVehicles;
        } catch (e) {
            error.value = "Impossible de charger le planning.";
        } finally {
            loading.value = false;
        }
    }

    return { loading, transports, drivers, unassignedVehicles, error, loadPlanning };
}

export interface PlanningCardPlacement {
    transport: PlanningTransport;
    /** Index (0-based) du premier jour affiché couvert par le trajet */
    columnStart: number;
    /** Index (0-based, inclus) du dernier jour affiché couvert par le trajet */
    columnEnd: number;
    lane: number;
    continuesBefore: boolean;
    continuesAfter: boolean;
}

export interface PlanningStaircaseLayout {
    placements: PlanningCardPlacement[];
    laneCount: number;
}

function transportSortTime(transport: PlanningTransport): number {
    const value = transport.departureStart ?? transport.arrivalStart;
    return value ? new Date(value).getTime() : Number.MAX_SAFE_INTEGER;
}

/**
 * Place les trajets d'un chauffeur « en escalier » : un trajet s'étend du jour de départ au jour d'arrivée,
 * et chaque trajet est posé sous tous les trajets précédents qui occupent l'un de ses jours.
 * Les segments d'un même service répartis sur plusieurs jours restent alignés (jamais plus haut que le segment précédent).
 * La lecture de haut en bas suit ainsi la chronologie, même pour les trajets à cheval sur plusieurs jours.
 */
export function buildStaircaseLayout(transports: PlanningTransport[], dayKeys: string[]): PlanningStaircaseLayout {
    const firstKey = dayKeys[0];
    const lastKey = dayKeys[dayKeys.length - 1];
    if (!firstKey || !lastKey) return { placements: [], laneCount: 0 };

    const dayIndexes = new Map(dayKeys.map((key, index) => [key, index]));
    const nextFreeLane = dayKeys.map(() => 0);
    const serviceLanes = new Map<number, { lane: number; columnEnd: number }>();
    const placements: PlanningCardPlacement[] = [];

    const sorted = [...transports].sort((first, second) =>
        transportSortTime(first) - transportSortTime(second) || first.routeId - second.routeId);

    for (const transport of sorted) {
        const startValue = transport.departureStart ?? transport.arrivalStart;
        if (!startValue) continue;

        const startKey = formatParisDateKey(startValue);
        const arrivalKey = transport.arrivalStart ? formatParisDateKey(transport.arrivalStart) : startKey;
        const endKey = arrivalKey < startKey ? startKey : arrivalKey;
        if (startKey > lastKey || endKey < firstKey) continue;

        const columnStart = startKey < firstKey ? 0 : dayIndexes.get(startKey);
        const columnEnd = endKey > lastKey ? dayKeys.length - 1 : dayIndexes.get(endKey);
        if (columnStart === undefined || columnEnd === undefined) continue;

        let lane = 0;
        // Un segment qui poursuit un service commencé un jour précédent reste aligné avec (ou sous) le segment précédent
        const previousSegment = serviceLanes.get(transport.id);
        if (previousSegment && previousSegment.columnEnd < columnStart) {
            lane = previousSegment.lane;
        }
        for (let column = columnStart; column <= columnEnd; column++) {
            lane = Math.max(lane, nextFreeLane[column] ?? 0);
        }
        for (let column = columnStart; column <= columnEnd; column++) {
            nextFreeLane[column] = lane + 1;
        }
        serviceLanes.set(transport.id, { lane, columnEnd });

        placements.push({
            transport,
            columnStart,
            columnEnd,
            lane,
            continuesBefore: startKey < firstKey,
            continuesAfter: endKey > lastKey,
        });
    }

    return { placements, laneCount: Math.max(0, ...nextFreeLane) };
}

export function createPlanningGridStyle(dayCount: number) {
    return {
        gridTemplateColumns: `240px 120px repeat(${dayCount}, minmax(220px, 1fr))`,
        minWidth: `${240 + 120 + dayCount * 220}px`,
        width: '100%',
    }
}