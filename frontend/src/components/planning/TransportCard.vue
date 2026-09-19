<script setup lang="ts">
import { computed } from "vue";

import type { PlanningTransport } from "@/models/planning/planning";
import { Truck, Container } from 'lucide-vue-next'

const props = defineProps<{
    transport: PlanningTransport;
}>();

const emit = defineEmits<{
    select: [transportId: number];
}>();

const tripLabel = computed(() => {
    return props.transport.emptyTrip ? "Trajet à vide" : "Trajet en charge";
});

const startTime = computed(() => {
    return new Intl.DateTimeFormat("fr-FR", {
        hour: "2-digit",
        minute: "2-digit",
    }).format(new Date(props.transport.plannedStart));
});

const endTime = computed(() => {
    return new Intl.DateTimeFormat("fr-FR", {
        hour: "2-digit",
        minute: "2-digit",
    }).format(new Date(props.transport.plannedEnd));
});

const formattedCost = computed(() => {
    const totalCost = props.transport.totalCost;

    if (totalCost === null || totalCost === undefined) {
        return "Coût indisponible";
    }

    return new Intl.NumberFormat("fr-FR", {
        style: "currency",
        currency: "EUR",
    }).format(totalCost);
});
</script>

<template>
    <button type="button" :class="[
        'w-full cursor-pointer rounded-lg border p-2 text-left shadow-sm transition hover:shadow-md',
        transport.emptyTrip
            ? 'border-red-200 bg-red-50 hover:border-red-300 hover:bg-red-100'
            : 'border-emerald-200 bg-emerald-50 hover:border-emerald-300 hover:bg-emerald-100'
    ]" @click="emit('select', transport.id)">
        <div class="flex items-center justify-between gap-2">
            <div class="min-w-0 truncate text-sm font-semibold text-slate-800"> {{ transport.name }} </div>

            <span :class="[
                'inline-flex shrink-0 items-center rounded-full border px-2 py-0.5 text-[10px] font-semibold',
                transport.emptyTrip
                    ? 'border-red-200 bg-red-100 text-red-700'
                    : 'border-emerald-200 bg-emerald-100 text-emerald-700'
            ]">
                {{ tripLabel }}
            </span>
        </div>

        <div class="mt-3 space-y-1 text-xs text-slate-600">
            <p class="truncate">
                ↑ {{ startTime }} — {{ transport.originName }}
            </p>

            <p class="truncate">
                ↓ {{ endTime }} — {{ transport.destinationName }}
            </p>
        </div>

        <div v-if="transport.tractorRegistration || transport.semiTrailerRegistration"
            class="mt-3 flex items-center gap-3 text-xs text-slate-600">
            <span v-if="transport.tractorRegistration" class="flex min-w-0 items-center gap-1.5">
                <Truck :size="14" class="shrink-0 text-slate-400" />
                <span class="truncate">{{ transport.tractorRegistration }}</span>
            </span>

            <span v-if="transport.semiTrailerRegistration" class="flex min-w-0 items-center gap-1.5">
                <Container :size="14" class="shrink-0 text-slate-400" />
                <span class="truncate">{{ transport.semiTrailerRegistration }}</span>
            </span>

        </div>


        <div class="mt-2 flex items-center justify-end border-t border-emerald-200 pt-2">
            <span class="text-xs font-semibold text-emerald-700">
                {{ formattedCost }}
            </span>
        </div>
    </button>
</template>