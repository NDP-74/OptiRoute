<template>
    <div class="grid min-w-0 border-t-2 border-slate-300 bg-slate-50" :style="gridStyle">
        <div class="sticky left-0 z-20 flex min-h-[56px] items-center border-r border-slate-300 bg-slate-50 p-4">
            <p class="text-sm font-semibold text-slate-800">
                Total
            </p>
        </div>

        <div
            class="sticky left-[240px] z-20 flex min-h-[56px] items-center justify-center border-r border-slate-300 bg-slate-50 px-3 py-4">
            <p class="text-sm font-semibold text-emerald-700">
                {{ formattedTotalCost }}
            </p>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import { createPlanningGridStyle } from "@/utils/planningUtils";

const props = defineProps<{
    totalCost: number;
    dayCount: number;
}>();

const gridStyle = computed(() => createPlanningGridStyle(props.dayCount));

const currencyFormatter = new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "EUR",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
});

const formattedTotalCost = computed<string>(() => {
    return currencyFormatter.format(props.totalCost);
});
</script>
