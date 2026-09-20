<script setup lang="ts">
import { computed } from "vue"
import { Container, Truck, Wrench } from "lucide-vue-next"

import type { VehicleEventResponse } from "@/models/vehicle/VehicleEvent"

const props = defineProps<{
    event: VehicleEventResponse
}>()

const emit = defineEmits<{
    select: [eventId: number]
}>()

const vehicleRegistration = computed(() => props.event.tractorRegistration ?? props.event.semiTrailerRegistration)
const isTractor = computed(() => props.event.tractorId !== null)
const formattedCost = computed(() => new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "EUR",
}).format(props.event.cost))
</script>

<template>
    <button type="button"
        class="w-full cursor-pointer rounded-lg border border-blue-200 bg-blue-50 p-2 text-left shadow-sm transition hover:border-blue-300 hover:bg-blue-100 hover:shadow-md"
        @click="emit('select', event.id)">
        <div class="flex items-center justify-between gap-2">
            <div class="flex min-w-0 items-center gap-1.5 text-sm font-semibold text-blue-900">
                <Wrench class="h-4 w-4 shrink-0 text-blue-600" />
                <span class="truncate">{{ event.supplier || "Événement véhicule" }}</span>
            </div>
            <span
                class="shrink-0 rounded-full border border-blue-200 bg-blue-100 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
                Événement
            </span>
        </div>

        <div class="mt-3 flex items-center gap-1.5 text-xs text-blue-800">
            <Truck v-if="isTractor" class="h-3.5 w-3.5 shrink-0" />
            <Container v-else class="h-3.5 w-3.5 shrink-0" />
            <span class="truncate">{{ vehicleRegistration }}</span>
        </div>

        <div class="mt-2 flex justify-end border-t border-blue-200 pt-2">
            <span class="text-xs font-semibold text-blue-700">{{ formattedCost }}</span>
        </div>
    </button>
</template>