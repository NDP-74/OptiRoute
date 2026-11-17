<script setup lang="ts">
import { computed, nextTick, onActivated, onMounted, ref, watch } from 'vue'
import { AlertTriangle, CalendarPlus, Clock, Fuel, MapPinned, PackageX, Receipt, Truck, User } from 'lucide-vue-next'

import HereMap from '@/components/maps/HereMap.vue'
import RouteForm from '@/components/maps/RouteForm.vue'
import AssignEventModal from '@/components/transports/AssignEventModal.vue'
import ConfirmDeleteModal from '@/components/ui/ConfirmDeleteModal.vue'
import { deleteEvent } from '@/api/eventApi'

import { updateTransportRoute } from '@/api/transportApi'
import { getDriver, getDrivers } from '@/api/driver/driverApi'
import { getTractors } from '@/api/vehicle/tractorApi'
import { getSemiTrailers } from '@/api/vehicle/semiTrailerApi'
import { getApiErrorMessage } from '@/api/utils'
import { useNotification } from '@/composables/useNotification'

import type { DriverSummary } from '@/models/driver/Driver'
import type { TractorSummary } from '@/models/vehicle/Tractor'
import type { SemiTrailerSummary } from '@/models/vehicle/SemiTrailer'
import type { RouteDto, RouteRequest, RouteResponse, RouteWaypoint } from '@/models/route/Route'
import type { TransportDetail, TransportRouteDetail } from '@/models/transport/TransportDetail'
import type { EventResponse } from '@/models/Event'

import { formatCurrency, formatDateTime, formatDistance, formatDurationSeconds } from '@/utils/formatters'
import { segmentWarnings } from '@/utils/segmentConsistency'

const props = defineProps<{
    transport: TransportDetail
    routeId: number
    events: EventResponse[]
}>()

const emit = defineEmits<{
    cancel: []
    saved: []
}>()

const notification = useNotification()
const mapRef = ref<InstanceType<typeof HereMap> | null>(null)
const formSession = ref(0)
const initialRequest = ref<RouteRequest | null>(null)
const routeRequest = ref<RouteRequest | null>(null)
const routeResponse = ref<RouteResponse | null>(null)
const selectedRouteIndex = ref(0)
const dirty = ref(false)
const isSaving = ref(false)
const showEventModal = ref(false)
const eventToDelete = ref<number | null>(null)
const deletingEvent = ref(false)

async function confirmDeleteEvent() {
    if (eventToDelete.value == null || deletingEvent.value) return
    deletingEvent.value = true
    try {
        await deleteEvent(eventToDelete.value)
        eventToDelete.value = null
        notification.success('Événement supprimé', 'L’événement a été supprimé et l’itinéraire recalculé.')
        emit('saved')
    } catch (error) {
        notification.error('Suppression impossible', getApiErrorMessage(error, 'L’événement n’a pas pu être supprimé.'))
    } finally {
        deletingEvent.value = false
    }
}

const driverId = ref<number | null>(null)
const tractorId = ref<number | null>(null)
const semiTrailerId = ref<number | null>(null)
const emptyTrip = ref(false)
const drivers = ref<DriverSummary[]>([])
const tractors = ref<TractorSummary[]>([])
const semiTrailers = ref<SemiTrailerSummary[]>([])

const segments = computed(() => props.transport?.routes ?? [])
const segmentIndex = computed(() => segments.value.findIndex(route => route.routeId === props.routeId))
const segment = computed<TransportRouteDetail | null>(() => segments.value[segmentIndex.value] ?? null)
const eventTransport = computed<TransportDetail | null>(() => {
    const route = segment.value
    if (!route) return null
    return {
        ...props.transport,
        ...route,
        revenue: route.revenue ?? 0,
        tractorRegistration: tractors.value.find(tractor => tractor.id === route.tractorId)?.registration
            ?? (route.tractorId === props.transport.tractorId ? props.transport.tractorRegistration : null),
        semiTrailerRegistration: semiTrailers.value.find(trailer => trailer.id === route.semiTrailerId)?.registration
            ?? (route.semiTrailerId === props.transport.semiTrailerId ? props.transport.semiTrailerRegistration : null),
    }
})
const hasUnsavedChanges = computed(() =>
    dirty.value || routeResponse.value !== null || driverId.value !== segment.value?.driverId)

function handleEventSaved() {
    showEventModal.value = false
    emit('saved')
}

function findRouteCost(route: TransportRouteDetail, label: string): number {
    return route.costs.vehicle.costs.find(cost => cost.label === label)?.amount ?? 0
}

// Itinéraire enregistré, réutilisé tant que rien n'impose un nouveau calcul (ex. simple changement de chauffeur)
const storedRoute = computed<RouteDto | null>(() => {
    const route = segment.value
    if (!route || route.distanceMeters == null || route.durationSeconds == null) return null

    const fuelCost = findRouteCost(route, 'Carburant')
    const tollCost = findRouteCost(route, 'Péages')
    return {
        type: route.routeType ?? undefined,
        duration: route.durationSeconds,
        baseDuration: route.durationSeconds,
        distanceMeters: route.distanceMeters,
        polyline: route.polyline ?? undefined,
        costs: { fuelCost, tollCost, totalCost: fuelCost + tollCost },
    }
})

const effectiveRoute = computed<RouteDto | null>(() => {
    if (dirty.value) return null
    return routeResponse.value?.routes[selectedRouteIndex.value] ?? storedRoute.value
})

const plannedTimes = computed(() => {
    const request = routeRequest.value
    const route = effectiveRoute.value
    if (!request?.routeTime || !route) return null

    const reference = new Date(request.routeTime).getTime()
    const start = request.timeMode === 'ARRIVAL' ? reference - route.duration * 1000 : reference
    return { start: new Date(start).toISOString(), end: new Date(start + route.duration * 1000).toISOString() }
})

// Avertissements sur la cohérence avec les segments voisins, qui ne sont jamais modifiés automatiquement
const warnings = computed(() => {
    const request = routeRequest.value
    const index = segmentIndex.value
    if (!request || index < 0) return []

    const edited = {
        originLat: request.origin.lat,
        originLng: request.origin.lng,
        destinationLat: request.destination.lat,
        destinationLng: request.destination.lng,
        departureStart: plannedTimes.value?.start ?? segment.value?.departureStart ?? null,
        arrivalStart: plannedTimes.value?.end ?? segment.value?.arrivalStart ?? null,
    }
    const neighbours = segments.value.map((route, position) => position === index ? edited : route)
    return segmentWarnings(neighbours)
        .filter(warning => warning.segmentIndex === index || warning.segmentIndex === index + 1)
        .map(warning => warning.message)
})

function eventWaypoints(events: EventResponse[]): RouteWaypoint[] {
    return events.flatMap(event =>
        event.pointOfInterestLatitude != null && event.pointOfInterestLongitude != null
            ? [{
                name: event.pointOfInterestLabel ?? event.costParameterLabel,
                address: event.pointOfInterestLabel ?? `${event.pointOfInterestLatitude}, ${event.pointOfInterestLongitude}`,
                lat: event.pointOfInterestLatitude,
                lng: event.pointOfInterestLongitude,
                eventId: event.id,
            }]
            : []
    )
}

function requestFromSegment(route: TransportRouteDetail): RouteRequest {
    return {
        origin: { name: route.originName, address: route.originAddress ?? '', lat: route.originLat, lng: route.originLng },
        destination: {
            name: route.destinationName,
            address: route.destinationAddress ?? '',
            lat: route.destinationLat,
            lng: route.destinationLng,
        },
        waypoints: [
            ...(route.waypoints ?? []).map(point => ({ ...point })),
            ...eventWaypoints(props.events.filter(event => event.routeId === route.routeId)),
        ],
        mode: route.routeType ?? 'FASTEST',
        emptyTrip: route.emptyTrip,
        routeTime: route.departureStart ?? route.arrivalStart ?? undefined,
        timeMode: route.departureStart || !route.arrivalStart ? 'DEPARTURE' : 'ARRIVAL',
        driverId: route.driverId ?? undefined,
        tractorId: route.tractorId ?? undefined,
        semiTrailerId: route.semiTrailerId ?? undefined,
    }
}

async function loadReferences() {
    if (drivers.value.length || tractors.value.length) return
    const [loadedDrivers, loadedTractors, loadedSemiTrailers] = await Promise.all([
        getDrivers(), getTractors(), getSemiTrailers(),
    ])
    drivers.value = loadedDrivers
    tractors.value = loadedTractors
    semiTrailers.value = loadedSemiTrailers
}

async function initialize() {
    const route = segment.value
    if (!route) return

    driverId.value = route.driverId
    tractorId.value = route.tractorId
    semiTrailerId.value = route.semiTrailerId
    emptyTrip.value = route.emptyTrip
    routeResponse.value = null
    selectedRouteIndex.value = 0
    initialRequest.value = requestFromSegment(route)
    routeRequest.value = initialRequest.value
    formSession.value++
    await nextTick()
    dirty.value = false

    mapRef.value?.setMarkers(initialRequest.value.origin, initialRequest.value.destination, initialRequest.value.waypoints)
    if (route.polyline) {
        const existingRoute = { polyline: route.polyline }
        mapRef.value?.displayRoutes([existingRoute], existingRoute)
    }
    void loadReferences().catch(error => notification.error('Chargement impossible',
        getApiErrorMessage(error, 'Les chauffeurs et véhicules n’ont pas pu être chargés.')))
}

onMounted(() => { void initialize() })

// Le panneau est conservé (KeepAlive) quand on change d'onglet : la carte doit se redimensionner à son retour
onActivated(async () => {
    await nextTick()
    mapRef.value?.resize()
})

// Le véhicule influence le calcul HERE (gabarit, péages) : il impose un nouveau calcul
watch([tractorId, semiTrailerId, emptyTrip], () => { dirty.value = true })

// Appelé uniquement sur un choix utilisateur, pour ne pas écraser le véhicule enregistré à l'ouverture
async function applyDriverVehicles() {
    const id = driverId.value
    if (id == null) return
    try {
        const driver = await getDriver(id)
        if (driverId.value !== id) return
        tractorId.value = driver.tractorId
        semiTrailerId.value = driver.semiTrailerId
    } catch (error) {
        notification.error('Chargement impossible', getApiErrorMessage(error, 'Le véhicule du chauffeur n’a pas pu être chargé.'))
    }
}

function handleRouteCalculated(data: { response: RouteResponse; request: RouteRequest }) {
    routeRequest.value = data.request
    routeResponse.value = data.response
    const index = data.response.routes.findIndex(route => route.type === segment.value?.routeType)
    selectedRouteIndex.value = index < 0 ? 0 : index
    dirty.value = false
    const routes = data.response.routes
    mapRef.value?.displayRoutes(routes, routes[selectedRouteIndex.value])
    mapRef.value?.setMarkers(data.request.origin, data.request.destination, data.request.waypoints)
}

function selectRoute(index: number) {
    const routes = routeResponse.value?.routes
    if (!routes?.[index]) return
    selectedRouteIndex.value = index
    mapRef.value?.displayRoutes(routes, routes[index])
}

function cancel() {
    if (!isSaving.value) emit('cancel')
}

async function save() {
    const request = routeRequest.value
    const route = effectiveRoute.value
    if (!request || !route || tractorId.value == null || isSaving.value) return
    if (!request.routeTime) {
        notification.error('Enregistrement impossible', 'Une heure de départ ou d’arrivée est requise.')
        return
    }

    isSaving.value = true
    try {
        await updateTransportRoute(props.transport.id, props.routeId, {
            driverId: driverId.value ?? undefined,
            tractorId: tractorId.value,
            semiTrailerId: semiTrailerId.value ?? undefined,
            emptyTrip: emptyTrip.value,
            routeTime: request.routeTime,
            timeMode: request.timeMode ?? 'DEPARTURE',
            originName: request.origin.name,
            originAddress: request.origin.address,
            originLat: request.origin.lat,
            originLng: request.origin.lng,
            destinationName: request.destination.name,
            destinationAddress: request.destination.address,
            destinationLat: request.destination.lat,
            destinationLng: request.destination.lng,
            waypoints: (request.waypoints ?? [])
                .filter(point => point.eventId == null)
                .map(({ lat, lng, name, address }) => ({ lat, lng, name, address })),
            selectedRoute: route,
        })
        notification.success('Trajet mis à jour', 'Seul ce trajet a été modifié.')
        emit('saved')
    } catch (error) {
        notification.error('Enregistrement impossible', getApiErrorMessage(error, 'Le trajet n’a pas pu être enregistré.'))
    } finally {
        isSaving.value = false
    }
}
</script>

<template>
    <div class="flex h-full min-h-0 flex-col">
        <div class="grid min-h-0 flex-1 gap-5 lg:grid-cols-3">
                <!-- AFFECTATION -->
                <section class="min-h-0 overflow-auto rounded-2xl border border-slate-200 bg-slate-50/60 p-4">
                    <div class="space-y-4">
                        <label class="block space-y-1.5">
                            <span
                                class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                <User :size="14" /> Chauffeur
                            </span>
                            <select v-model="driverId" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm"
                                @change="applyDriverVehicles">
                                <option :value="null">Aucun chauffeur</option>
                                <option v-for="driver in drivers" :key="driver.id" :value="driver.id">
                                    {{ driver.firstName }} {{ driver.lastName }}
                                </option>
                            </select>
                        </label>
                        <label class="block space-y-1.5">
                            <span
                                class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                <Truck :size="14" /> Tracteur
                            </span>
                            <select v-model="tractorId" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm">
                                <option :value="null">Sélectionner un tracteur</option>
                                <option v-for="tractor in tractors" :key="tractor.id" :value="tractor.id">
                                    {{ tractor.registration }}
                                </option>
                            </select>
                        </label>
                        <label class="block space-y-1.5">
                            <span
                                class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                <Truck :size="14" /> Semi-remorque
                            </span>
                            <select v-model="semiTrailerId" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm">
                                <option :value="null">Aucune semi-remorque</option>
                                <option v-for="semiTrailer in semiTrailers" :key="semiTrailer.id" :value="semiTrailer.id">
                                    {{ semiTrailer.registration }}
                                </option>
                            </select>
                        </label>
                        <label
                            class="flex cursor-pointer items-center justify-between rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700">
                            <span class="flex items-center gap-2">
                                <PackageX :size="16" class="text-slate-400" /> Trajet à vide
                            </span>
                            <input v-model="emptyTrip" type="checkbox" class="h-4 w-4" />
                        </label>
                    </div>
                </section>

                <!-- ITINÉRAIRE -->
                <section class="flex min-h-0 flex-col">
                    <div class="custom-scrollbar min-h-0 flex-1 overflow-auto rounded-2xl border border-slate-200 p-4">
                        <RouteForm :key="formSession" :initial-request="initialRequest" removable-events
                            v-model:tractor-id="tractorId" v-model:semi-trailer-id="semiTrailerId"
                            v-model:driver-id="driverId" v-model:empty-trip="emptyTrip"
                            @changed="dirty = true" @route-calculated="handleRouteCalculated"
                            @remove-event="eventToDelete = $event" />
                        <p v-if="dirty" class="mt-3 text-xs text-amber-700" role="status">
                            Recalculez l’itinéraire pour prendre en compte vos modifications.
                        </p>
                    </div>
                    <div v-if="!dirty && routeResponse?.routes?.length" class="mt-4 space-y-2">
                        <button v-for="(route, index) in routeResponse.routes" :key="index" type="button"
                            class="w-full rounded-xl border p-3 text-left transition"
                            :class="index === selectedRouteIndex ? 'border-blue-500 bg-blue-50' : 'border-slate-200 hover:bg-slate-50'"
                            @click="selectRoute(index)">
                            <span class="rounded-md bg-emerald-100 px-2 py-1 text-xs font-semibold text-emerald-700">
                                {{ route.type === 'CHEAPEST' ? 'Route la plus économique' : 'Route la plus rapide' }}
                            </span>
                            <div class="mt-2 flex items-center justify-between gap-3">
                                <span class="font-semibold">{{ formatCurrency(route.costs.totalCost) }}</span>
                                <span class="text-sm font-semibold">{{ formatDurationSeconds(route.duration) }}</span>
                            </div>
                            <div class="mt-1 text-xs text-slate-500">{{ formatDistance(route.distanceMeters) }}</div>
                        </button>
                    </div>
                </section>

                <!-- CARTE & RÉSUMÉ -->
                <section class="flex min-h-0 min-w-0 flex-col gap-4">
                    <div class="min-h-0 flex-1 overflow-hidden rounded-2xl border border-slate-200 bg-slate-100 shadow-inner">
                        <HereMap ref="mapRef" @route-selected="selectRoute" />
                    </div>

                    <div v-if="warnings.length" class="shrink-0 rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800"
                        role="alert">
                        <p v-for="warning in warnings" :key="warning" class="flex items-start gap-2">
                            <AlertTriangle :size="14" class="mt-0.5 shrink-0" /> {{ warning }}
                        </p>
                    </div>

                    <div class="shrink-0 rounded-2xl border border-slate-200 bg-slate-50/60 p-4 text-sm">
                        <template v-if="effectiveRoute">
                            <div class="grid grid-cols-2 gap-3">
                                <div class="flex items-center gap-2">
                                    <MapPinned :size="16" class="shrink-0 text-slate-400" />
                                    <div>
                                        <p class="text-slate-500">Distance</p>
                                        <p class="font-semibold">{{ formatDistance(effectiveRoute.distanceMeters) }}</p>
                                    </div>
                                </div>
                                <div class="flex items-center gap-2">
                                    <Clock :size="16" class="shrink-0 text-slate-400" />
                                    <div>
                                        <p class="text-slate-500">Durée</p>
                                        <p class="font-semibold">{{ formatDurationSeconds(effectiveRoute.duration) }}</p>
                                    </div>
                                </div>
                                <div class="flex items-center gap-2">
                                    <Fuel :size="16" class="shrink-0 text-slate-400" />
                                    <div>
                                        <p class="text-slate-500">Carburant</p>
                                        <p class="font-semibold">{{ formatCurrency(effectiveRoute.costs.fuelCost) }}</p>
                                    </div>
                                </div>
                                <div class="flex items-center gap-2">
                                    <Receipt :size="16" class="shrink-0 text-slate-400" />
                                    <div>
                                        <p class="text-slate-500">Péage</p>
                                        <p class="font-semibold">{{ formatCurrency(effectiveRoute.costs.tollCost) }}</p>
                                    </div>
                                </div>
                            </div>
                            <p v-if="plannedTimes" class="mt-3 text-xs text-slate-500">
                                {{ formatDateTime(plannedTimes.start) }} → {{ formatDateTime(plannedTimes.end) }}
                            </p>
                        </template>
                        <p v-else class="text-slate-500">Calculez l’itinéraire pour afficher le résumé.</p>
                    </div>
                </section>
            </div>

            <div class="flex shrink-0 items-center justify-between gap-3 pt-4">
                <p class="text-xs text-slate-500">
                    <template v-if="segments.length > 1">Les autres trajets du service ne sont pas modifiés.</template>
                </p>
                <div class="flex gap-3">
                    <button type="button" class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                        :disabled="isSaving" @click="cancel">
                        Annuler
                    </button>
                    <button type="button"
                        class="inline-flex items-center gap-2 rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                        :disabled="isSaving || hasUnsavedChanges || !eventTransport"
                        :title="hasUnsavedChanges ? 'Enregistrez les modifications du trajet avant d’assigner un événement.' : undefined"
                        @click="showEventModal = true">
                        <CalendarPlus class="h-4 w-4" />
                        Assigner un événement
                    </button>
                    <button type="button"
                        class="rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                        :disabled="!effectiveRoute || tractorId == null || isSaving" @click="save">
                        {{ isSaving ? 'Enregistrement…' : `Mettre à jour le trajet ${segmentIndex + 1}` }}
                    </button>
                </div>
            </div>
    </div>
    <AssignEventModal :show="showEventModal" :transport="eventTransport"
        @close="showEventModal = false" @saved="handleEventSaved" />
    <ConfirmDeleteModal :show="eventToDelete !== null" :loading="deletingEvent" title="Supprimer l’événement"
        message="Supprimer cet événement et son point de passage ? Le trajet sera recalculé et les modifications non enregistrées de cet onglet seront remplacées."
        @close="!deletingEvent && (eventToDelete = null)" @confirm="confirmDeleteEvent" />
</template>
