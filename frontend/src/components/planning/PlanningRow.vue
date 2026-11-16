<template>
    <div class="grid min-h-[140px] min-w-0 border-b border-slate-200 bg-white" :style="gridStyle">
        <InfoCell :style="{ gridColumn: 1, gridRow: fullHeightRow }" :driver-name="driver.name"
            :vehicle-registrations="driver.vehicleRegistrations" :is-unassigned="driver.id === -1" />

        <CostCell :style="{ gridColumn: 2, gridRow: fullHeightRow }" :total-cost="driver.totalCost"
            :total-revenue="driver.totalRevenue" :is-unassigned="driver.id === -1"
            @select="emit('cost-detail-select', driver.id)" />

        <DayCell v-for="(day, index) in days" :key="day.key"
            :style="{ gridColumn: index + FIRST_DAY_COLUMN, gridRow: fullHeightRow }"
            :selection-enabled="daySelectionMode" :selected="selectedDayKeys.has(`${driver.id}:${day.key}`)"
            :selection-label="`Sélectionner la journée du ${day.label} pour ${driver.name}`"
            @selection-toggle="emit('day-selection-toggle', { driverId: driver.id, dayKey: day.key })" />

        <div v-for="placement in layout.placements" :key="placement.transport.routeId"
            :class="['relative z-10 min-w-0 px-2 py-1', daySelectionMode ? 'pointer-events-none' : '']"
            :style="{
                gridColumn: `${placement.columnStart + FIRST_DAY_COLUMN} / ${placement.columnEnd + FIRST_DAY_COLUMN + 1}`,
                gridRow: placement.lane + FIRST_LANE_ROW,
            }">
            <TransportCard :transport="placement.transport" :selection-mode="daySelectionMode"
                :multi-day="placement.columnEnd > placement.columnStart || placement.continuesBefore || placement.continuesAfter"
                :continues-before="placement.continuesBefore" :continues-after="placement.continuesAfter"
                @select="handleTransportSelect" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import CostCell from "@/components/planning/CostCell.vue";
import InfoCell from "@/components/planning/DriverCell.vue";
import DayCell from "@/components/planning/DayCell.vue";
import TransportCard from "@/components/planning/TransportCard.vue";

import { buildStaircaseLayout, createPlanningGridStyle } from "@/utils/planningUtils";

import type { PlanningDay, PlanningDriver } from "@/models/planning/planning";

const FIRST_DAY_COLUMN = 3;
// La 1re ligne de la grille est un espacement (agrandi en mode sélection pour laisser la place aux cases à cocher)
const FIRST_LANE_ROW = 2;

const emit = defineEmits<{
    "transport-select": [selection: { serviceId: number; routeId: number }];
    "cost-detail-select": [driverId: number];
    "day-selection-toggle": [selection: { driverId: number; dayKey: string }];
}>();

const props = defineProps<{
    driver: PlanningDriver;
    days: PlanningDay[];
    daySelectionMode: boolean;
    selectedDayKeys: Set<string>;
}>();

const layout = computed(() => buildStaircaseLayout(
    Object.values(props.driver.days).flat(),
    props.days.map((day) => day.key),
));

// Espacement haut + une ligne par niveau d'escalier + une ligne de remplissage pour la hauteur minimale
const rowCount = computed(() => layout.value.laneCount + 2);
const fullHeightRow = computed(() => `1 / span ${rowCount.value}`);

const gridStyle = computed(() => ({
    ...createPlanningGridStyle(props.days.length),
    gridTemplateRows: [
        props.daySelectionMode ? "2.25rem" : "0.25rem",
        ...(layout.value.laneCount > 0 ? [`repeat(${layout.value.laneCount}, auto)`] : []),
        "minmax(0.25rem, 1fr)",
    ].join(" "),
}));

function handleTransportSelect(selection: { serviceId: number; routeId: number }): void {
    if (!props.daySelectionMode) {
        emit("transport-select", selection);
    }
}
</script>