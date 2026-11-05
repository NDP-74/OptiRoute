<template>
    <div class="grid min-w-0 border-b border-slate-200 bg-white" :style="gridStyle">
        <InfoCell :driver-name="driver.name" :vehicle-registrations="driver.vehicleRegistrations"
            :is-unassigned="driver.id === -1" />

        <CostCell :total-cost="driver.totalCost" :total-revenue="driver.totalRevenue" :is-unassigned="driver.id === -1"
            @select="emit('cost-detail-select', driver.id)" />

        <DayCell v-for="day in days" :key="day.key" :transports="driver.days[day.key] ?? []"
            :selection-enabled="daySelectionMode" :selected="selectedDayKeys.has(`${driver.id}:${day.key}`)"
            :selection-label="`Sélectionner la journée du ${day.label} pour ${driver.name}`"
            @transport-select="emit('transport-select', $event)"
            @selection-toggle="emit('day-selection-toggle', { driverId: driver.id, dayKey: day.key })" />
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import CostCell from "@/components/planning/CostCell.vue";
import InfoCell from "@/components/planning/DriverCell.vue";
import DayCell from "@/components/planning/DayCell.vue";

import { createPlanningGridStyle } from "@/utils/planningUtils";

import type { PlanningDay, PlanningDriver } from "@/models/planning/planning";

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

const gridStyle = computed(() =>
    createPlanningGridStyle(props.days.length)
)
</script>