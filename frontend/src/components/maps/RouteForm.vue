<script setup lang="ts">
import { computed, ref, reactive, watch } from 'vue'
import { ArrowUpDown, Clock, Plus, SlidersHorizontal, X } from 'lucide-vue-next'
import HereAutocompleteInput from './HereAutocompleteInput.vue'

import { calculateRoute } from '@/api/here/mapsApi'

import type { Position } from '@/models/route/Position'
import type { RouteRequest } from '@/models/route/Route'

const props = defineProps<{
    initialRequest?: RouteRequest | null
}>()

const tractorId = defineModel<number | null>('tractorId', { default: null })
const semiTrailerId = defineModel<number | null>('semiTrailerId', { default: null })
const driverId = defineModel<number | null>('driverId', { default: null })
const emptyTrip = defineModel<boolean>('emptyTrip', { default: false })

//Variables
const departureMode = ref('NOW')
const isSubmitting = ref(false)
const MAX_WAYPOINTS = 10

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

// Convertit un ISO string (UTC ou avec offset) en valeur locale attendue par <input type="datetime-local">
function toDateTimeLocalValue(isoValue: string): string | null {
    const date = new Date(isoValue)
    if (Number.isNaN(date.getTime())) return null

    const pad = (n: number) => String(n).padStart(2, '0')

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function applyInitialRequest(request: RouteRequest | null | undefined) {
    if (!request) return

    form.origin = toPlace(request.origin)
    form.destination = toPlace(request.destination)
    form.waypoints = (request.waypoints ?? []).map(toPlace)
    form.routeTime = request.routeTime ? toDateTimeLocalValue(request.routeTime) : null
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

function swapWithPrevious(index: number) {
    if (index <= 0) return

    const current = getRowValue(index)
    const previous = getRowValue(index - 1)

    setRowValue(index, previous)
    setRowValue(index - 1, current)
}

const rowCount = computed(() => form.waypoints.length + 2)
const lastRowIndex = computed(() => rowCount.value - 1)

function getRowValue(index: number) {
    if (index === 0) return form.origin
    if (index === lastRowIndex.value) return form.destination

    return form.waypoints[index - 1]
}

function setRowValue(index: number, value: any) {
    if (index === 0) {
        form.origin = value
        return
    }

    if (index === lastRowIndex.value) {
        form.destination = value
        return
    }

    form.waypoints[index - 1] = value
}

function rowLabel(index: number) {
    if (index === 0) return 'Départ'
    if (index === lastRowIndex.value) return 'Arrivée'

    return `Étape ${index}`
}

function removeWaypointAt(index: number) {
    form.waypoints.splice(index - 1, 1)
}

function addWaypoint() {
    if (form.waypoints.length >= MAX_WAYPOINTS) {
        return
    }

    form.waypoints.push(null)
}

async function submit() {

    if (!form.origin || !form.destination || isSubmitting.value) {
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

            tractorId: tractorId.value ?? undefined,
            semiTrailerId: semiTrailerId.value ?? undefined,
            driverId: driverId.value ?? undefined,
            emptyTrip: emptyTrip.value,
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
        <div class="relative grid gap-2" :style="{ gridTemplateRows: `repeat(${rowCount}, auto)` }">
            <div v-for="index in rowCount" :key="index - 1" class="flex items-center gap-2" :style="{ gridRow: index }">
                <div class="flex-1">
                    <HereAutocompleteInput :model-value="getRowValue(index - 1)"
                        @update:model-value="setRowValue(index - 1, $event)" :label="rowLabel(index - 1)" />
                </div>

                <div class="flex h-9 w-9 shrink-0 items-center justify-center">
                    <button v-if="index - 1 === 0" type="button" @click="addWaypoint"
                        :disabled="form.waypoints.length >= MAX_WAYPOINTS"
                        :title="form.waypoints.length >= MAX_WAYPOINTS ? `Limite de ${MAX_WAYPOINTS} étapes atteinte` : 'Ajouter une étape'"
                        class="flex h-9 w-9 items-center justify-center rounded-full border border-slate-300 bg-white text-slate-600 shadow-sm transition hover:border-slate-400 hover:bg-slate-50 hover:text-slate-900 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-300 disabled:shadow-none"
                        aria-label="Ajouter une étape">
                        <Plus :size="16" :stroke-width="2" aria-hidden="true" />
                    </button>

                    <button v-else-if="index - 1 !== lastRowIndex" type="button" @click="removeWaypointAt(index - 1)"
                        class="flex h-9 w-9 items-center justify-center rounded-full border border-red-200 bg-red-50 text-red-600 shadow-sm transition hover:bg-red-100"
                        aria-label="Supprimer cette étape" title="Supprimer cette étape">
                        <X :size="16" :stroke-width="2" aria-hidden="true" />
                    </button>
                </div>
            </div>

            <button v-for="index in rowCount - 1" :key="`swap-${index}`" type="button" @click="swapWithPrevious(index)"
                :style="{ gridRow: `${index} / span 2`, top: '50%' }"
                class="absolute right-11 z-10 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full border border-slate-300 bg-white text-slate-600 shadow-sm transition hover:border-slate-400 hover:bg-slate-50 hover:text-slate-900"
                aria-label="Inverser avec le champ précédent" title="Inverser avec le champ précédent">
                <ArrowUpDown :size="16" :stroke-width="2" aria-hidden="true" />
            </button>
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