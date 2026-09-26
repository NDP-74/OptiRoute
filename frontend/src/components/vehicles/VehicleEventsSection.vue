<script setup lang="ts">
import { computed, ref, watch } from "vue"
import { CalendarDays, RefreshCw, Wrench } from "lucide-vue-next"

import { getVehicleEventsBySemiTrailer, getVehicleEventsByTractor } from "@/api/vehicleEventApi"
import { getApiErrorMessage } from "@/api/utils"
import type { VehicleEventResponse } from "@/models/vehicle/VehicleEvent"
import { formatCurrency, formatDate } from "@/utils/formatters"

const props = defineProps<{
    vehicleType: "tractor" | "semiTrailer"
    vehicleId: number
}>()

const events = ref<VehicleEventResponse[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
let currentRequestId = 0

const sortedEvents = computed(() => [...events.value].sort((first, second) =>
    second.eventDate.localeCompare(first.eventDate) || second.id - first.id,
))

async function loadEvents() {
    const requestId = ++currentRequestId
    loading.value = true
    error.value = null

    try {
        const loadedEvents = props.vehicleType === "tractor"
            ? await getVehicleEventsByTractor(props.vehicleId)
            : await getVehicleEventsBySemiTrailer(props.vehicleId)

        if (requestId === currentRequestId) {
            events.value = loadedEvents
        }
    } catch (exception) {
        if (requestId === currentRequestId) {
            error.value = getApiErrorMessage(exception, "Impossible de charger les événements du véhicule.")
        }
    } finally {
        if (requestId === currentRequestId) {
            loading.value = false
        }
    }
}

watch(() => [props.vehicleType, props.vehicleId], () => void loadEvents(), { immediate: true })
</script>

<template>
    <section>
        <div class="mb-3 flex items-center justify-between gap-3">
            <div class="flex min-w-0 items-center gap-2">
                <CalendarDays class="h-4 w-4 shrink-0 text-slate-500" />
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">Événements associés</h3>
            </div>
        </div>

        <div v-if="loading"
            class="flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-8 text-sm text-slate-500">
            <span class="h-4 w-4 animate-spin rounded-full border-2 border-slate-300 border-t-blue-600" />
            Chargement des événements...
        </div>

        <div v-else-if="error"
            class="flex items-center justify-between gap-3 rounded-xl border border-red-200 bg-red-50 p-4">
            <p class="text-sm text-red-700">{{ error }}</p>
            <button type="button"
                class="inline-flex shrink-0 items-center gap-2 rounded-lg bg-white px-3 py-2 text-sm font-medium text-red-700 shadow-sm hover:bg-red-100"
                @click="loadEvents">
                <RefreshCw class="h-4 w-4" />Réessayer
            </button>
        </div>

        <div v-else-if="sortedEvents.length === 0"
            class="rounded-xl border border-dashed border-slate-300 bg-white px-4 py-8 text-center">
            <Wrench class="mx-auto h-5 w-5 text-slate-400" />
            <p class="mt-2 text-sm font-medium text-slate-700">Aucun événement enregistré</p>
            <p class="mt-1 text-xs text-slate-500">Les événements de ce véhicule apparaîtront ici.</p>
        </div>

        <ul v-else class="custom-scrollbar max-h-80 space-y-2 overflow-y-auto pr-1"
            aria-label="Historique des événements">
            <li v-for="event in sortedEvents" :key="event.id"
                class="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
                <div class="flex items-start justify-between gap-3">
                    <div class="flex min-w-0 items-start gap-3">
                        <span
                            class="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-700">
                            <Wrench class="h-4 w-4" />
                        </span>
                        <div class="min-w-0">
                            <p class="truncate text-sm font-semibold text-slate-800">{{ event.supplier || "Fournisseur"
                                + " non renseigné" }}</p>
                            <p class="mt-0.5 text-xs text-slate-500">{{ formatDate(event.eventDate) }}</p>
                        </div>
                    </div>
                    <span class="inline-flex shrink-0 items-center gap-1 text-sm font-semibold text-slate-800">
                        {{ formatCurrency(event.cost) }}
                    </span>
                </div>
            </li>
        </ul>
    </section>
</template>