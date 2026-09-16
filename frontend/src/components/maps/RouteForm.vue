<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ArrowUpDown } from 'lucide-vue-next'
import HereAutocompleteInput from './HereAutocompleteInput.vue'

import { calculateRoute } from '@/api/here/mapsApi'
import { getTractors } from "@/api/vehicle/tractorApi"
import { getSemiTrailers } from "@/api/vehicle/semiTrailerApi"

import type { Position } from '@/models/route/Position'
import type { TractorSummary } from "@/models/vehicle/Tractor"
import type { SemiTrailerSummary } from "@/models/vehicle/SemiTrailer"
import { formatVehicleLabel } from "@/utils/vehicleUtils"
import { formatDurationMinutes } from "@/utils/formatters"

//Variables
const departureMode = ref('NOW')
const tractors = ref<TractorSummary[]>([])
const semiTrailers = ref<SemiTrailerSummary[]>([])
const isSubmitting = ref(false)
const MAX_WAYPOINTS = 4

const emit = defineEmits(['route-calculated'])

const form = reactive({
    origin: null as any,
    destination: null as any,
    waypoints: [] as any[],

    routeTime: null as any,

    mode: 'FASTEST',
    emptyTrip: false,

    tractorId: null,
    semiTrailerId: null,
})

// Functions

function toPosition(place: {
    name: string
    address: string
    position: {
        lat: number
        lng: number
    }
}): Position {
    return {
        name: place.name,
        address: place.address,
        lat: place.position.lat,
        lng: place.position.lng
    }
}

function swapLocations() {
    const origin = form.origin
    form.origin = form.destination
    form.destination = origin
}

function addWaypoint() {
    if (form.waypoints.length >= MAX_WAYPOINTS) {
        return
    }

    form.waypoints.push(null)
}

async function submit() {

    if (!form.origin || !form.destination || !form.tractorId || isSubmitting.value) {
        return
    }

    if (form.waypoints.filter(Boolean).length > MAX_WAYPOINTS) {
        return
    }

    isSubmitting.value = true

    try {
        const effectiveRouteTime = toOffsetDateTime(form.routeTime) ?? new Date().toISOString()

        const payload = {
            origin: toPosition(form.origin),
            destination: toPosition(form.destination),
            waypoints: form.waypoints.filter(Boolean).map(toPosition),

            routeTime: effectiveRouteTime,
            timeMode: departureMode.value === 'ARRIVALTIME' ? 'ARRIVAL' : 'DEPARTURE',

            mode: form.mode,
            emptyTrip: form.emptyTrip,

            tractorId: form.tractorId,
            semiTrailerId: form.semiTrailerId,
        }

        const response = await calculateRoute(payload)
        emit('route-calculated',
            {
                response,
                request: payload
            }
        )
    } catch (e) {
        console.error(e)
    } finally {
        isSubmitting.value = false
    }
}

function toOffsetDateTime(value: string) {
    if (departureMode.value === 'NOW' || !value) return null

    return new Date(value).toISOString()
}

onMounted(async () => {
    tractors.value = await getTractors()
    semiTrailers.value = await getSemiTrailers()
})

</script>

<template>
    <div class="space-y-5">

        <!-- LOCATIONS -->
        <div class="relative">
            <HereAutocompleteInput v-model="form.origin" label="Départ" />
            <div class="mt-5">
                <HereAutocompleteInput v-model="form.destination" label="Arrivée" />
            </div>

            <!-- SWAP -->
            <button type="button" @click="swapLocations"
                class="absolute right-0 top-[58%] z-10 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full border border-slate-300 bg-white text-slate-600 shadow-sm transition hover:border-slate-400 hover:bg-slate-50 hover:text-slate-900"
                aria-label="Inverser le départ et l'arrivée" title="Inverser le départ et l'arrivée">
                <ArrowUpDown :size="16" :stroke-width="2" aria-hidden="true" />
            </button>
        </div>

        <!-- WAYPOINTS -->
        <div class="space-y-3">
            <div class="flex items-center justify-end gap-3">
                <button type="button" @click="addWaypoint" :disabled="form.waypoints.length >= MAX_WAYPOINTS"
                    class="rounded-lg border border-slate-300 bg-white px-2 py-1 text-xs font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50">
                    {{ form.waypoints.length >= MAX_WAYPOINTS ? 'Limite atteinte (4)' : '+ Ajouter une étape' }}
                </button>
            </div>

            <div v-for="(waypoint, index) in form.waypoints" :key="index" class="flex items-start gap-2">
                <div class="flex-1">
                    <HereAutocompleteInput v-model="form.waypoints[index]" :label="`Étape ${index + 1}`" />
                </div>

                <button type="button" @click="form.waypoints.splice(index, 1)"
                    class="mt-8 rounded-lg border border-red-200 bg-red-50 px-2 py-2 text-sm text-red-600 transition hover:bg-red-100"
                    aria-label="Supprimer cette étape" title="Supprimer cette étape">
                    ×
                </button>
            </div>
        </div>

        <!-- MODE -->
        <div class="space-y-3">

            <label class="block text-sm font-medium">
                Mode
            </label>

            <select v-model="form.mode" class="w-full rounded-xl border border-slate-300 p-3">
                <option value="FASTEST">
                    Plus rapide
                </option>

                <option value="CHEAPEST">
                    Plus économique
                </option>

            </select>
        </div>

        <!-- TRIP TYPE -->
        <label class="flex items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-3">
            <input v-model="form.emptyTrip" type="checkbox"
                class="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500" />

            <span class="text-sm font-medium text-slate-700">
                Trajet à vide
            </span>
        </label>

        <!-- DEPARTURE TIME -->
        <div class="space-y-3">

            <label class="block text-sm font-medium">
                Heure
            </label>

            <select v-model="departureMode" class="w-full rounded-xl border border-slate-300 p-3">
                <option value="NOW">
                    Départ maintenant
                </option>

                <option value="PLANNED">
                    Départ prévu à
                </option>

                <option value="ARRIVALTIME">
                    Arrivée à
                </option>
            </select>

            <input v-if="departureMode === 'PLANNED' || departureMode === 'ARRIVALTIME'" v-model="form.routeTime"
                type="datetime-local" class="w-full rounded-xl border border-slate-300 p-3" />

        </div>

        <!-- VEHICULE -->
        <div class="space-y-3">

            <label class="block text-sm font-medium">
                Tracteur
            </label>

            <select v-model="form.tractorId" class="w-full rounded-xl border border-slate-300 p-3">
                <option :value="null">
                    Sélectionner un tracteur
                </option>

                <option v-for="tractor in tractors" :key="tractor.id" :value="tractor.id">
                    {{ tractor.registration }} - {{ formatVehicleLabel(tractor.brand, tractor.model) }}
                </option>
            </select>

        </div>

        <div class="space-y-3">

            <label class="block text-sm font-medium">
                Semi-remorque
            </label>

            <select v-model="form.semiTrailerId" class="w-full rounded-xl border border-slate-300 p-3">
                <option :value="null">
                    Sélectionner une semi-remorque
                </option>

                <option v-for="semiTrailer in semiTrailers" :key="semiTrailer.id" :value="semiTrailer.id">
                    {{ semiTrailer.registration }} - {{ formatVehicleLabel(semiTrailer.brand, semiTrailer.model) }}
                </option>
            </select>

        </div>

        <!-- BUTTON -->
        <button @click="submit" :disabled="isSubmitting"
            class="w-full rounded-xl p-3 text-white transition disabled:cursor-not-allowed disabled:opacity-80"
            :class="isSubmitting ? 'bg-slate-700' : 'bg-slate-900 hover:bg-slate-800'">
            <span class="flex items-center justify-center gap-2">
                <svg v-if="isSubmitting" class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none"
                    xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                    <path class="opacity-75"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A8 8 0 0112 4v4a4 4 0 00-2.236 6.97l-3.764 2.321z"
                        fill="currentColor" />
                </svg>

                <span>
                    {{ isSubmitting ? 'Calcul en cours...' : "Calculer l'itinéraire" }}
                </span>
            </span>
        </button>

    </div>
</template>