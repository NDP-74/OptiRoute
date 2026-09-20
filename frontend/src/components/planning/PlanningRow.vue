<template>
    <div class="grid min-w-0 border-b border-slate-200 bg-white" :style="gridStyle">
        <InfoCell :driver-name="driver.name" :vehicle-registrations="driver.vehicleRegistrations"
            :is-unassigned="driver.id === -1" />

        <CostCell :total-cost="driver.totalCost" :total-revenue="driver.totalRevenue" :is-unassigned="driver.id === -1"
            @select="emit('cost-detail-select', driver.id)" />

        <DayCell v-for="day in days" :key="day.key" :transports="driver.days[day.key] ?? []"
            :events="driver.events[day.key] ?? []" @transport-select="emit('transport-select', $event)"
            @event-select="emit('event-select', $event)" />
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
    "transport-select": [transportId: number];
    "event-select": [eventId: number];
    "cost-detail-select": [driverId: number];
}>();

const props = defineProps<{
    driver: PlanningDriver;
    days: PlanningDay[];
}>();

const gridStyle = computed(() =>
    createPlanningGridStyle(props.days.length)
)
</script>