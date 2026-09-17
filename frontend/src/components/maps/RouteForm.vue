<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ArrowUpDown, Clock, SlidersHorizontal } from 'lucide-vue-next'
import HereAutocompleteInput from './HereAutocompleteInput.vue'

import { calculateRoute } from '@/api/here/mapsApi'

import type { Position } from '@/models/route/Position'
import type { RouteRequest } from '@/models/route/Route'

const props = defineProps<{
    initialRequest?: RouteRequest | null
}>()

const tractorId = defineModel<number | null>('tractorId', { default: null })
const semiTrailerId = defineModel<number | null>('semiTrailerId', { default: null })
const emptyTrip = defineModel<boolean>('emptyTrip', { default: false })

//Variables
const departureMode = ref('NOW')
const isSubmitting = ref(false)
const MAX_WAYPOINTS = 3

const emit = defineEmits(['route-calculated'])

const form = reactive({
    origin: null as any,
    destination: null as any,
    waypoints: [] as any[],

    routeTime: null as any,

    mode: 'FASTEST',
})

function toPlace(position: Position) {
    return {
        name: position.name ?? '',
        address: position.address ?? '',
        position: {
            lat: position.lat,
            lng: position.lng,
        },
    }
}

function applyInitialRequest(request: RouteRequest | null | undefined) {
    if (!request) return

    form.origin = toPlace(request.origin)
    form.destination = toPlace(request.destination)
    form.waypoints = (request.waypoints ?? []).map(toPlace)
    form.routeTime = request.routeTime ? request.routeTime.slice(0, 16) : null
    form.mode = request.mode
    departureMode.value = request.timeMode === 'ARRIVAL' ? 'ARRIVALTIME' : 'PLANNED'
}

watch(() => props.initialRequest, applyInitialRequest, { immediate: true })

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

    if (!form.origin || !form.destination || !tractorId.value || isSubmitting.value) {
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
            emptyTrip: emptyTrip.value,

            tractorId: tractorId.value,
            semiTrailerId: semiTrailerId.value,
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
                    {{ form.waypoints.length >= MAX_WAYPOINTS ? 'Limite atteinte (3)' : '+ Ajouter une étape' }}
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
        <div class="space-y-1.5">

            <label class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                <SlidersHorizontal :size="14" />
                Mode
            </label>

            <select v-model="form.mode"
                class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20">
                <option value="FASTEST">
                    Plus rapide
                </option>

                <option value="CHEAPEST">
                    Plus économique
                </option>

            </select>
        </div>

        <!-- DEPARTURE TIME -->
        <div class="space-y-1.5">

            <label class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                <Clock :size="14" />
                Heure
            </label>

            <select v-model="departureMode"
                class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20">
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
                type="datetime-local"
                class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20" />

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