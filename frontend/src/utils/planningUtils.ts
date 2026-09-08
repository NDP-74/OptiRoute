import { ref } from "vue";

import { getPlanning } from "@/api/planningApi";

import type { PlanningRequest, PlanningResponse } from "@/models/planning/planning";

export function usePlanning() {

    const loading = ref(false);
    const transports = ref<PlanningResponse["transports"]>([]);
    const drivers = ref<PlanningResponse["drivers"]>([]);
    const error = ref<string | null>(null);

    async function loadPlanning(request: PlanningRequest) {
        try {
            loading.value = true;
            error.value = null;
            const planning = await getPlanning(request);
            transports.value = planning.transports;
            drivers.value = planning.drivers;
        } catch (e) {
            error.value = "Impossible de charger le planning.";
        } finally {
            loading.value = false;
        }
    }

    return { loading, transports, drivers, error, loadPlanning };
}

export function createPlanningGridStyle(dayCount: number) {
    return {
        gridTemplateColumns: `240px 120px repeat(${dayCount}, minmax(220px, 1fr))`,
        minWidth: `${240 + 120 + dayCount * 220}px`,
        width: '100%',
    }
}