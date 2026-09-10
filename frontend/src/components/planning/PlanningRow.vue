<template>
    <div class="grid min-w-0 border-b border-slate-200 bg-white" :style="gridStyle">
        <InfoCell :driver-name="driver.name" :transport-count="transportCount"
            :vehicle-registrations="driver.vehicleRegistrations" />

        <CostCell :total-cost="driver.totalCost" />

        <DayCell v-for="day in days" :key="day.key" :transports="driver.days[day.key] ?? []"
            @transport-select="emit('transport-select', $event)" />
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
}>();

const props = defineProps<{
    driver: PlanningDriver;
    days: PlanningDay[];
}>();

const transportCount = computed<number>(() => {
    return Object.values(props.driver.days).reduce((total, dayTransports) => total + dayTransports.length, 0);
});

const gridStyle = computed(() =>
    createPlanningGridStyle(props.days.length)
)
</script>