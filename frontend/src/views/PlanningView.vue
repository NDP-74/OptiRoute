<template>
    <div class="relative flex h-full min-h-0 flex-col overflow-hidden bg-slate-100">
        <PlanningToolbar :start-date="selectedStartDate" :end-date="selectedEndDate"
            :day-selection-mode="daySelectionMode" :selected-day-count="selectedDayKeys.size"
            :is-completing-days="isCompletingDays" @update:range="handleRangeChange" @today-range="applyTodayRange"
            @create-route="openRouteModal" @start-day-selection="startDaySelection"
            @cancel-day-selection="cancelDaySelection" @complete-days="completeSelectedDays" />

        <PlanningGrid :drivers="planningDrivers" :days="days" :loading="loading" :error="error"
            :day-selection-mode="daySelectionMode" :selected-day-keys="selectedDayKeys" @retry="loadCurrentPeriod"
            @transport-select="openTransport" @cost-detail-select="openCostDetail"
            @day-selection-toggle="toggleDaySelection" />

        <CostDetailDrawer :open="selectedCostDetailDriverId !== null" :driver="selectedCostDetailDriver"
            @close="selectedCostDetailDriverId = null" />

        <TransportDetailDrawer :open="selectedTransport !== null" :transport-id="selectedTransport?.serviceId ?? null"
            :route-id="selectedTransport?.routeId ?? null" @close="closeTransport" @deleted="handleTransportDeleted"
            @updated="loadCurrentPeriod" @edit="openEditModal" />

        <PlanningRouteModal :show="showRouteModal" :editing-transport="editingTransport" :events="editingEvents"
            @close="closeRouteModal" @saved="handleRouteSaved" />
    </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import PlanningGrid from "@/components/planning/PlanningGrid.vue";
import PlanningToolbar from "@/components/planning/PlanningToolbar.vue";
import PlanningRouteModal from "@/components/planning/PlanningRouteModal.vue";
import CostDetailDrawer from "@/components/planning/CostDetailDrawer.vue";
import TransportDetailDrawer from "@/components/transports/TransportDetailDrawer.vue";

import { usePlanning } from "@/utils/planningUtils";
import { getTransportById } from "@/api/planningApi";
import { createTransportFromRoute } from "@/api/transportApi";
import { calculateRoute } from "@/api/here/mapsApi";
import { getApiErrorMessage } from "@/api/utils";
import { useNotification } from "@/composables/useNotification";

import type { PlanningDay, PlanningDriver, PlanningTransport, } from "@/models/planning/planning";
import type { TransportDetail } from "@/models/transport/TransportDetail";
import type { EventResponse } from "@/models/Event";
import type { RouteRequest, RouteResponse } from "@/models/route/Route";
import type { CreateTransportFromRouteRequest } from "@/models/transport/TransportRequest";
import { formatParisDateKey } from "@/utils/formatters";


const selectedTransport = ref<{ serviceId: number; routeId: number } | null>(null);
const selectedCostDetailDriverId = ref<number | null>(null);
const showRouteModal = ref(false);
const editingTransport = ref<TransportDetail | null>(null);
const editingEvents = ref<EventResponse[]>([]);
const daySelectionMode = ref(false);
const selectedDayKeys = ref<Set<string>>(new Set());
const isCompletingDays = ref(false);
const notification = useNotification();

function daySelectionKey(driverId: number, dayKey: string): string {
    return `${driverId}:${dayKey}`;
}

function startDaySelection(): void {
    daySelectionMode.value = true;
}

function cancelDaySelection(): void {
    selectedDayKeys.value = new Set();
    daySelectionMode.value = false;
}

function toggleDaySelection(selection: { driverId: number; dayKey: string }): void {
    const key = daySelectionKey(selection.driverId, selection.dayKey);
    const nextSelection = new Set(selectedDayKeys.value);

    if (nextSelection.has(key)) {
        nextSelection.delete(key);
    } else {
        nextSelection.add(key);
    }

    selectedDayKeys.value = nextSelection;
}

function getTimestamp(value?: string | null): number | null {
    if (!value) {
        return null;
    }

    const timestamp = new Date(value).getTime();
    return Number.isFinite(timestamp) ? timestamp : null;
}

function transportDetailKey(transport: PlanningTransport): string {
    return `${transport.id}:${transport.routeId}`;
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error && !("response" in error)
        ? error.message
        : getApiErrorMessage(error, "Une erreur est survenue.");
}

async function completeSelectedDays(): Promise<void> {
    if (isCompletingDays.value || selectedDayKeys.value.size === 0) {
        return;
    }

    const selectedCells = planningDrivers.value.flatMap((driver) =>
        days.value
            .filter((day) => selectedDayKeys.value.has(daySelectionKey(driver.id, day.key)))
            .map((day) => ({ driver, dayKey: day.key }))
    );
    const detailCache = new Map<string, Promise<TransportDetail>>();
    let createdCount = 0;
    let noGapCount = 0;
    let insufficientTimeCount = 0;
    let unavailableRouteCount = 0;
    const failures: string[] = [];

    function loadTransportDetail(transport: PlanningTransport): Promise<TransportDetail> {
        const key = transportDetailKey(transport);
        let detail = detailCache.get(key);

        if (!detail) {
            detail = getTransportById(transport.id, transport.routeId);
            detailCache.set(key, detail);
        }

        return detail;
    }

    isCompletingDays.value = true;
    daySelectionMode.value = false;

    try {
        for (const { driver, dayKey } of selectedCells) {
            const transportsForDay = [...(driver.days[dayKey] ?? [])].sort((first, second) => {
                return (getTimestamp(first.departureStart ?? first.arrivalStart) ?? Number.MAX_SAFE_INTEGER)
                    - (getTimestamp(second.departureStart ?? second.arrivalStart) ?? Number.MAX_SAFE_INTEGER);
            });
            let dayGapCount = 0;

            for (let index = 0; index < transportsForDay.length - 1; index++) {
                const previousTransport = transportsForDay[index];
                const nextTransport = transportsForDay[index + 1];
                if (!previousTransport || !nextTransport) {
                    continue;
                }

                const previousArrival = getTimestamp(previousTransport.arrivalStart);
                const nextDeparture = getTimestamp(nextTransport.departureStart);

                if (previousArrival === null || nextDeparture === null || nextDeparture <= previousArrival) {
                    continue;
                }

                try {
                    const [previousDetail, nextDetail] = await Promise.all([
                        loadTransportDetail(previousTransport),
                        loadTransportDetail(nextTransport),
                    ]);

                    if (previousDetail.driverId === null || previousDetail.tractorId === null) {
                        throw new Error("Le trajet précédent n’a pas de chauffeur ni de tracteur assigné.");
                    }

                    const actualPreviousArrival = getTimestamp(previousDetail.arrivalStart) ?? previousArrival;
                    const unloadingDurationMinutes = previousDetail.serviceTiming.unloadingDurationMinutes ?? 0;
                    if (!Number.isFinite(unloadingDurationMinutes) || unloadingDurationMinutes < 0) {
                        throw new Error("La durée de déchargement du trajet précédent est invalide.");
                    }

                    const emptyTripDeparture = actualPreviousArrival + unloadingDurationMinutes * 60_000;
                    if (nextDeparture <= actualPreviousArrival) {
                        continue;
                    }

                    dayGapCount++;
                    if (nextDeparture <= emptyTripDeparture) {
                        insufficientTimeCount++;
                        continue;
                    }

                    const routeRequest: RouteRequest = {
                        origin: {
                            name: previousDetail.destinationName,
                            address: previousDetail.destinationAddress ?? "",
                            lat: previousDetail.destinationLat,
                            lng: previousDetail.destinationLng,
                        },
                        destination: {
                            name: nextDetail.originName,
                            address: nextDetail.originAddress ?? "",
                            lat: nextDetail.originLat,
                            lng: nextDetail.originLng,
                        },
                        tractorId: previousDetail.tractorId,
                        semiTrailerId: previousDetail.semiTrailerId ?? undefined,
                        driverId: previousDetail.driverId,
                        mode: "FASTEST",
                        emptyTrip: true,
                        routeTime: new Date(emptyTripDeparture).toISOString(),
                        timeMode: "DEPARTURE",
                    };
                    const routeResponse: RouteResponse = await calculateRoute(routeRequest);
                    if (routeResponse.routes.length === 0) {
                        unavailableRouteCount++;
                        continue;
                    }

                    const availableDuration = (nextDeparture - emptyTripDeparture) / 1000;
                    const selectedRoute = routeResponse.routes
                        .filter((candidate) => Number.isFinite(candidate.duration) && candidate.duration <= availableDuration)
                        .sort((first, second) => first.duration - second.duration)[0];

                    if (!selectedRoute) {
                        insufficientTimeCount++;
                        continue;
                    }

                    const request: CreateTransportFromRouteRequest = {
                        transport: {
                            name: "Trajet à vide",
                            driverId: previousDetail.driverId,
                            tractorId: previousDetail.tractorId,
                            semiTrailerId: previousDetail.semiTrailerId ?? undefined,
                            emptyTrip: true,
                            routeTime: new Date(emptyTripDeparture).toISOString(),
                            timeMode: "DEPARTURE",
                            originName: routeRequest.origin.name,
                            originAddress: routeRequest.origin.address,
                            originLat: routeRequest.origin.lat,
                            originLng: routeRequest.origin.lng,
                            destinationName: routeRequest.destination.name,
                            destinationAddress: routeRequest.destination.address,
                            destinationLat: routeRequest.destination.lat,
                            destinationLng: routeRequest.destination.lng,
                            revenue: null,
                            waypoints: [],
                        },
                        selectedRoute,
                    };

                    await createTransportFromRoute(request);
                    createdCount++;
                } catch (error) {
                    failures.push(`${driver.name} (${dayKey}) : ${getErrorMessage(error)}`);
                }
            }

            if (dayGapCount === 0) {
                noGapCount++;
            }
        }

        if (createdCount > 0) {
            await loadCurrentPeriod();
        }

        if (failures.length > 0) {
            const examples = failures.slice(0, 2).join(" ");
            const remaining = failures.length > 2 ? ` (+${failures.length - 2} autre(s) erreur(s))` : "";
            notification.error(
                "Complément partiel",
                `${createdCount} trajet(s) créé(s), ${failures.length} échec(s). ${examples}${remaining}`
            );
        } else if (createdCount > 0) {
            notification.success(
                "Journées complétées",
                `${createdCount} trajet(s) à vide créé(s).`
            );
        } else {
            notification.warning(
                "Aucun trajet créé",
                noGapCount === selectedCells.length && insufficientTimeCount === 0
                    ? "Aucune journée sélectionnée ne contient un créneau entre deux trajets."
                    : `Aucun itinéraire créé : ${insufficientTimeCount} créneau(x) trop court(s), ${unavailableRouteCount} itinéraire(s) indisponible(s).`
            );
        }
    } finally {
        selectedDayKeys.value = new Set();
        daySelectionMode.value = false;
        isCompletingDays.value = false;
    }
}

function closePlanningDrawers(): void {
    selectedTransport.value = null;
    selectedCostDetailDriverId.value = null;
}

function openTransport(selection: { serviceId: number; routeId: number }): void {
    closePlanningDrawers();
    selectedTransport.value = selection;
}
function closeTransport(): void { selectedTransport.value = null; }
function openCostDetail(driverId: number): void {
    closePlanningDrawers();
    selectedCostDetailDriverId.value = driverId;
}
function openRouteModal(): void { showRouteModal.value = true; }
function closeRouteModal(): void {
    showRouteModal.value = false;
    editingTransport.value = null;
    editingEvents.value = [];
}
function openEditModal(transport: TransportDetail, events: EventResponse[]): void {
    editingTransport.value = transport;
    editingEvents.value = [...events];
    closeTransport();
    showRouteModal.value = true;
}


const { transports, drivers, unassignedVehicles, loading, error, loadPlanning } = usePlanning();

const handleTransportDeleted = async (): Promise<void> => {
    closeTransport();
    await loadCurrentPeriod();
};

const handleRouteSaved = async (): Promise<void> => {
    closeRouteModal();
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

const UNASSIGNED_VEHICLES_ROW_ID = -1;

const planningDrivers = computed<PlanningDriver[]>(() => {
    const driversMap = new Map<number, PlanningDriver>();

    drivers.value.forEach((driverSummary) => {
        driversMap.set(driverSummary.id, {
            id: driverSummary.id,
            name: driverSummary.name,
            vehicleRegistrations: [driverSummary.tractorRegistration, driverSummary.semiTrailerRegistration].filter(
                (registration): registration is string => !!registration
            ),
            totalCost: driverSummary.salaryForNonTransportDays,
            totalRevenue: 0,
            driverCost: driverSummary.salaryForNonTransportDays,
            structureCost: 0,
            vehicleCost: 0,
            days: {},
        });
    });

    const sortedTransports = [...transports.value].sort((first, second) => {
        return (new Date(first.departureStart ?? 0).getTime() - new Date(second.departureStart ?? 0).getTime());
    });

    sortedTransports.forEach((transport: PlanningTransport) => {
        let driver = driversMap.get(transport.driverId);

        if (!driver) {
            return;
        }

        const plannedDate = transport.departureStart ?? transport.arrivalStart;
        if (!plannedDate) {
            return;
        }
        const dayKey = formatParisDateKey(plannedDate);

        if (!driver.days[dayKey]) {
            driver.days[dayKey] = [];
        }

        driver.days[dayKey].push(transport);

        driver.totalCost += transport.totalCost;
        driver.totalRevenue += transport.revenue;
        driver.driverCost += transport.driverCost;
        driver.structureCost += transport.structureCost;
        driver.vehicleCost += transport.vehicleCost;
    });

    const sortedDrivers = Array.from(driversMap.values()).sort((first, second) => {
        return first.name.localeCompare(second.name, "fr", { sensitivity: "base" });
    });

    if (unassignedVehicles.value.registrations.length > 0) {
        const unassignedDriver: PlanningDriver = {
            id: UNASSIGNED_VEHICLES_ROW_ID,
            name: "Véhicules non utilisés",
            vehicleRegistrations: [...unassignedVehicles.value.registrations],
            totalCost: unassignedVehicles.value.depreciationCost,
            totalRevenue: 0,
            driverCost: 0,
            structureCost: 0,
            vehicleCost: unassignedVehicles.value.depreciationCost,
            vehicleDepreciationCosts: unassignedVehicles.value.vehicles,
            days: {},
        };

        sortedDrivers.push(unassignedDriver);
    }

    return sortedDrivers;
});

const selectedCostDetailDriver = computed<PlanningDriver | null>(() => {
    if (selectedCostDetailDriverId.value === null) {
        return null;
    }

    return planningDrivers.value.find((driver) => driver.id === selectedCostDetailDriverId.value) ?? null;
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
    () => {
        selectedDayKeys.value = new Set();
        daySelectionMode.value = false;
        void loadCurrentPeriod();
    },
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