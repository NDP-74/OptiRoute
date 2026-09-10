<template>
    <div class="relative flex h-full min-h-0 flex-col overflow-hidden bg-slate-100">
        <PlanningToolbar :start-date="selectedStartDate" :end-date="selectedEndDate" @update:range="handleRangeChange"
            @today-range="applyTodayRange" />

        <PlanningGrid :drivers="planningDrivers" :days="days" :loading="loading" :error="error"
            @retry="loadCurrentPeriod" @transport-select="openTransport" />

        <TransportDetailDrawer :open="selectedTransportId !== null" :transport-id="selectedTransportId"
            @close="closeTransport" @deleted="handleTransportDeleted" />
    </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import PlanningGrid from "@/components/planning/PlanningGrid.vue";
import PlanningToolbar from "@/components/planning/PlanningToolbar.vue";
import TransportDetailDrawer from "@/components/transports/TransportDetailDrawer.vue";

import { usePlanning } from "@/utils/planningUtils";

import type { PlanningDay, PlanningDriver, PlanningTransport, } from "@/models/planning/planning";


const selectedTransportId = ref<number | null>(null);

function openTransport(transportId: number): void { selectedTransportId.value = transportId; }
function closeTransport(): void { selectedTransportId.value = null; }


const { transports, drivers, loading, error, loadPlanning } = usePlanning();

const handleTransportDeleted = async (): Promise<void> => {
    closeTransport();
    await loadCurrentPeriod();
};

/*
 * Utilitaires de dates.
 */
function addDays(date: Date, numberOfDays: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + numberOfDays);

    return result;
}

function startOfDay(date: Date): Date {
    const result = new Date(date);
    result.setHours(0, 0, 0, 0);

    return result;
}

function formatDateKey(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

function capitalize(value: string): string {
    if (!value) {
        return value;
    }

    return value.charAt(0).toUpperCase() + value.slice(1);
}

const route = useRoute();
const router = useRouter();

const today = startOfDay(new Date());
const PLANNING_RANGE_STORAGE_KEY = "optiroute:lastPlanningRange";

function parseQueryDate(value: unknown): Date | null {
    const rawValue = Array.isArray(value) ? value[0] : value;

    if (typeof rawValue !== "string" || !rawValue) {
        return null;
    }

    const parsed = new Date(`${rawValue}T00:00:00`);

    if (Number.isNaN(parsed.getTime())) {
        return null;
    }

    return startOfDay(parsed);
}

function persistRange(start: Date, end: Date): void {
    try {
        window.localStorage.setItem(
            PLANNING_RANGE_STORAGE_KEY,
            JSON.stringify({
                start: formatDateKey(start),
                end: formatDateKey(end),
            })
        );
    } catch {
        // Ignore storage errors silently.
    }
}

function restoreRangeFromStorage(): { start: Date; end: Date } | null {
    try {
        const rawValue = window.localStorage.getItem(PLANNING_RANGE_STORAGE_KEY);

        if (!rawValue) {
            return null;
        }

        const parsed = JSON.parse(rawValue) as { start?: string; end?: string };
        const start = parseQueryDate(parsed.start);
        const end = parseQueryDate(parsed.end);

        if (!start || !end) {
            return null;
        }

        return { start, end };
    } catch {
        return null;
    }
}

const initialStoredRange = restoreRangeFromStorage();

const selectedStartDate = ref<Date>(
    parseQueryDate(route.query.startDate) ?? initialStoredRange?.start ?? new Date(today)
);

const selectedEndDate = ref<Date>(
    parseQueryDate(route.query.endDate) ?? initialStoredRange?.end ?? addDays(today, 2)
);

const numberOfDisplayedDays = computed<number>(() => {
    const millisecondsPerDay = 24 * 60 * 60 * 1000;
    const difference = selectedEndDate.value.getTime() - selectedStartDate.value.getTime();

    return Math.floor(difference / millisecondsPerDay) + 1;
});

const days = computed<PlanningDay[]>(() => {
    const formatter = new Intl.DateTimeFormat("fr-FR", {
        weekday: "short",
        day: "2-digit",
        month: "2-digit",
    });

    return Array.from(
        { length: numberOfDisplayedDays.value, },
        (_, index) => {
            const date = addDays(selectedStartDate.value, index);

            return {
                key: formatDateKey(date),
                label: capitalize(formatter.format(date).replace(".", "")),
                date,
            };
        }
    );
});

const planningDrivers = computed<PlanningDriver[]>(() => {
    const driversMap = new Map<number, PlanningDriver>();

    drivers.value.forEach((driverSummary) => {
        driversMap.set(driverSummary.id, {
            id: driverSummary.id,
            name: driverSummary.name,
            tractorRegistration: driverSummary.tractorRegistration,
            semiTrailerRegistration: driverSummary.semiTrailerRegistration,
            totalCost: driverSummary.salaryForNonTransportDays,
            days: {},
        });
    });

    const sortedTransports = [...transports.value].sort((first, second) => {
        return (new Date(first.plannedStart).getTime() - new Date(second.plannedStart).getTime());
    });

    sortedTransports.forEach((transport: PlanningTransport) => {
        let driver = driversMap.get(transport.driverId);

        if (!driver) {
            return;
        }

        const dayKey = formatDateKey(new Date(transport.plannedStart));

        if (!driver.days[dayKey]) {
            driver.days[dayKey] = [];
        }

        driver.days[dayKey].push(transport);

        driver.totalCost += transport.totalCost;
    });

    return Array.from(driversMap.values()).sort((first, second) => {
        return first.name.localeCompare(second.name, "fr", { sensitivity: "base" });
    });
});

async function loadCurrentPeriod(): Promise<void> {
    const endDateExclusive = addDays(selectedEndDate.value, 1);

    await loadPlanning({
        startDate: formatDateKey(selectedStartDate.value),
        endDate: formatDateKey(endDateExclusive),
    });
}

function syncUrlRange(start: Date, end: Date): void {
    const nextStart = formatDateKey(start);
    const nextEnd = formatDateKey(end);

    persistRange(start, end);

    const currentStart = Array.isArray(route.query.startDate) ? route.query.startDate[0] : route.query.startDate;
    const currentEnd = Array.isArray(route.query.endDate) ? route.query.endDate[0] : route.query.endDate;

    if (currentStart === nextStart && currentEnd === nextEnd) {
        return;
    }

    void router.replace({
        name: "planning",
        query: {
            startDate: nextStart,
            endDate: nextEnd,
        },
    });
}

function handleRangeChange(range: [Date, Date]): void {
    const [start, requestedEnd] = range;

    const normalizedStart = startOfDay(start);
    const normalizedEnd = startOfDay(requestedEnd);

    const maximumEnd = addDays(normalizedStart, 6);

    selectedStartDate.value = normalizedStart;
    selectedEndDate.value = normalizedEnd > maximumEnd ? maximumEnd : normalizedEnd;
    syncUrlRange(selectedStartDate.value, selectedEndDate.value);
}

function applyTodayRange(startOffset: number, additionalDays: number): void {
    const currentToday = startOfDay(new Date());

    selectedStartDate.value = addDays(currentToday, startOffset);
    selectedEndDate.value = addDays(currentToday, startOffset + additionalDays);
    syncUrlRange(selectedStartDate.value, selectedEndDate.value);
}

watch(
    [() => formatDateKey(selectedStartDate.value), () => formatDateKey(selectedEndDate.value),],
    () => { void loadCurrentPeriod(); },
    {
        immediate: true,
    }
);

watch(
    () => route.query,
    (query) => {
        const nextStart = parseQueryDate(query.startDate);
        const nextEnd = parseQueryDate(query.endDate);

        if (nextStart && nextEnd) {
            const normalizedStart = startOfDay(nextStart);
            const normalizedEnd = startOfDay(nextEnd);
            const maximumEnd = addDays(normalizedStart, 6);
            const clampedEnd = normalizedEnd > maximumEnd ? maximumEnd : normalizedEnd;

            if (
                formatDateKey(selectedStartDate.value) !== formatDateKey(normalizedStart) ||
                formatDateKey(selectedEndDate.value) !== formatDateKey(clampedEnd)
            ) {
                selectedStartDate.value = normalizedStart;
                selectedEndDate.value = clampedEnd;
                persistRange(selectedStartDate.value, selectedEndDate.value);
            }

            return;
        }

        const storedRange = restoreRangeFromStorage();

        if (!storedRange) {
            return;
        }

        const normalizedStart = startOfDay(storedRange.start);
        const normalizedEnd = startOfDay(storedRange.end);

        if (
            formatDateKey(selectedStartDate.value) !== formatDateKey(normalizedStart) ||
            formatDateKey(selectedEndDate.value) !== formatDateKey(normalizedEnd)
        ) {
            selectedStartDate.value = normalizedStart;
            selectedEndDate.value = normalizedEnd;
            persistRange(selectedStartDate.value, selectedEndDate.value);
        }
    },
    {
        immediate: true,
    }
);
</script>