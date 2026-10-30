<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'

import AppModal from '@/components/ui/AppModal.vue'
import HereMap from '@/components/maps/HereMap.vue'
import RouteForm from '@/components/maps/RouteForm.vue'

import { createTransportFromRoute, updateTransportFromRoute } from '@/api/transportApi'
import { calculateRoute } from '@/api/here/mapsApi'
import { getCustomers } from '@/api/customerApi'
import { getDriver, getDrivers } from '@/api/driver/driverApi'
import { getTractors } from '@/api/vehicle/tractorApi'
import { getSemiTrailers } from '@/api/vehicle/semiTrailerApi'
import { getApiErrorMessage } from '@/api/utils'
import { useNotification } from '@/composables/useNotification'

import type { CustomerSummary } from '@/models/Customer'
import type { DriverSummary } from '@/models/driver/Driver'
import type { TractorSummary } from '@/models/vehicle/Tractor'
import type { SemiTrailerSummary } from '@/models/vehicle/SemiTrailer'
import type { RouteDto, RouteRequest, RouteResponse } from '@/models/route/Route'
import type { CreateTransportFromRouteRequest, TimeWindows } from '@/models/transport/TransportRequest'
import type { TransportDetail } from '@/models/transport/TransportDetail'
import type { EventResponse } from '@/models/Event'

import { formatCurrency, formatDateTime, formatDistance, formatDurationSeconds } from '@/utils/formatters'

import {
    Building2,
    Clock,
    Fuel,
    ListChecks,
    MapPinned,
    PackageX,
    Route as RouteIcon,
    Truck,
    User,
    Wallet,
    X,
    Euro,
    Receipt,
} from 'lucide-vue-next'

const props = defineProps<{
    show: boolean
    initialRouteRequest?: RouteRequest | null
    initialRouteResponse?: RouteResponse | null
    initialSelectedRouteIndex?: number
    editingTransport?: TransportDetail | null
    events?: EventResponse[]
}>()

const emit = defineEmits<{
    close: []
    saved: []
}>()

const notification = useNotification()
const mapRef = ref<InstanceType<typeof HereMap> | null>(null)
const routeResponse = ref<RouteResponse | null>(null)
const routeRequest = ref<RouteRequest | null>(null)
const selectedRouteIndex = ref(0)
const driverId = ref<number>()
const customerId = ref<number>()
const tractorId = ref<number | null>(null)
const semiTrailerId = ref<number | null>(null)
const emptyTrip = ref(false)
const revenue = ref<number | null>(null)
const drivers = ref<DriverSummary[]>([])
const customers = ref<CustomerSummary[]>([])
const tractors = ref<TractorSummary[]>([])
const semiTrailers = ref<SemiTrailerSummary[]>([])
const isSaving = ref(false)
const loadingDurationMinutes = ref<number | ''>('')
const unloadingDurationMinutes = ref<number | ''>('')
const windowFields = [
    { key: 'departureStart', label: 'Départ minimal' },
    { key: 'departureEnd', label: 'Départ maximal' },
    { key: 'arrivalStart', label: 'Arrivée minimale' },
    { key: 'arrivalEnd', label: 'Arrivée maximale' },
] as const
const serviceWindows = reactive({ departureStart: '', departureEnd: '', arrivalStart: '', arrivalEnd: '' })

function localDate(value?: string | null): string {
    if (!value) return ''
    const date = new Date(value)
    const pad = (part: number) => String(part).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function serializeWindows(values: typeof serviceWindows): TimeWindows {
    return Object.fromEntries(windowFields.map(({ key }) => [key, values[key] ? new Date(values[key]).toISOString() : null]))
}

const selectedRoute = computed(() => routeResponse.value?.routes[selectedRouteIndex.value])

// Route de l'estimation existante, utilisée tant qu'aucun nouveau calcul n'a été fait en modification
const fallbackRoute = computed<RouteDto | null>(() => {
    const transport = props.editingTransport

    if (!transport || transport.distanceMeters == null || transport.durationSeconds == null) {
        return null
    }

    const findCost = (label: string) =>
        transport.costs.vehicle.costs.find(cost => cost.label === label)?.amount ?? 0

    return {
        type: transport.routeType ?? undefined,
        duration: transport.durationSeconds,
        baseDuration: transport.durationSeconds,
        distanceMeters: transport.distanceMeters,
        polyline: transport.polyline ?? undefined,
        costs: {
            fuelCost: findCost('Carburant'),
            tollCost: findCost('Péages'),
            totalCost: findCost('Carburant') + findCost('Péages'),
        },
    }
})

const effectiveRoute = computed<RouteDto | null>(() => selectedRoute.value ?? fallbackRoute.value)

const transportTitle = computed(() => {
    const customerName = customers.value.find(customer => customer.id === customerId.value)?.name?.trim()

    if (customerName) {
        return customerName
    }

    if (routeRequest.value?.emptyTrip) {
        return 'Trajet à vide'
    }

    const originName = routeRequest.value?.origin.name?.trim() || 'Départ'
    const destinationName = routeRequest.value?.destination.name?.trim() || 'Arrivée'
    return `${originName} - ${destinationName}`
})

const plannedStart = computed(() => {
    if (!routeRequest.value?.routeTime) return ''

    const value = new Date(routeRequest.value.routeTime)
    if (routeRequest.value.timeMode === 'ARRIVAL' && effectiveRoute.value) {
        value.setSeconds(value.getSeconds() - effectiveRoute.value.duration)
    }

    return value.toISOString()
})

const plannedEnd = computed(() => {
    if (!routeRequest.value?.routeTime) return ''
    if (routeRequest.value.timeMode === 'ARRIVAL') return routeRequest.value.routeTime

    const value = new Date(routeRequest.value.routeTime)
    value.setSeconds(value.getSeconds() + (effectiveRoute.value?.duration ?? 0))
    return value.toISOString()
})

function close() {
    if (!isSaving.value) emit('close')
}

function handleRouteCalculated(data: { response: RouteResponse; request: RouteRequest }) {
    routeResponse.value = data.response
    routeRequest.value = data.request
    selectedRouteIndex.value = 0

    const routes = data.response.routes ?? []
    if (routes.length > 0) {
        mapRef.value?.displayRoutes(routes, routes[0])
        mapRef.value?.setMarkers(data.request.origin, data.request.destination, data.request.waypoints)
    }
}

function selectRoute(index: number) {
    const routes = routeResponse.value?.routes
    if (!routes?.[index]) return

    selectedRouteIndex.value = index
    mapRef.value?.displayRoutes(routes, routes[index])
}

function routeRequestFromTransport(transport: TransportDetail): RouteRequest {
    return {
        origin: {
            name: transport.originName,
            address: transport.originAddress ?? '',
            lat: transport.originLat,
            lng: transport.originLng,
        },
        destination: {
            name: transport.destinationName,
            address: transport.destinationAddress ?? '',
            lat: transport.destinationLat,
            lng: transport.destinationLng,
        },
        waypoints: [...(transport.waypoints ?? []), ...(props.events ?? []).flatMap(event =>
            event.pointOfInterestLatitude != null && event.pointOfInterestLongitude != null
                ? [{
                    name: event.pointOfInterestLabel ?? event.costParameterLabel,
                    address: event.pointOfInterestLabel ?? `${event.pointOfInterestLatitude}, ${event.pointOfInterestLongitude}`,
                    lat: event.pointOfInterestLatitude,
                    lng: event.pointOfInterestLongitude,
                    eventId: event.id,
                }]
                : []
        )],
        mode: transport.routeType ?? 'FASTEST',
        emptyTrip: transport.emptyTrip,
        routeTime: transport.arrivalStart ?? transport.departureStart ?? undefined,
        timeMode: transport.arrivalStart ? 'ARRIVAL' : 'DEPARTURE',
        driverId: transport.driverId ?? undefined,
    }
}

async function initializeFromProps() {
    loadingDurationMinutes.value = props.editingTransport?.serviceTiming?.loadingDurationMinutes ?? ''
    unloadingDurationMinutes.value = props.editingTransport?.serviceTiming?.unloadingDurationMinutes ?? ''
    for (const { key } of windowFields) {
        serviceWindows[key] = localDate(props.editingTransport?.serviceTiming?.[key])
    }

    if (props.editingTransport) {
        const transport = props.editingTransport

        routeRequest.value = routeRequestFromTransport(transport)
        routeResponse.value = null
        selectedRouteIndex.value = 0

        driverId.value = transport.driverId ?? undefined
        customerId.value = transport.customerId ?? undefined
        tractorId.value = transport.tractorId
        semiTrailerId.value = transport.semiTrailerId
        emptyTrip.value = transport.emptyTrip
        revenue.value = transport.revenue
    } else {
        routeRequest.value = props.initialRouteRequest ?? null
        routeResponse.value = props.initialRouteResponse ?? null
        selectedRouteIndex.value = props.initialSelectedRouteIndex ?? 0
        tractorId.value = props.initialRouteRequest?.tractorId ?? null
        semiTrailerId.value = props.initialRouteRequest?.semiTrailerId ?? null
        emptyTrip.value = props.initialRouteRequest?.emptyTrip ?? false
    }

    await nextTick()

    if (routeRequest.value && routeResponse.value?.routes.length) {
        const routes = routeResponse.value.routes
        const index = Math.min(selectedRouteIndex.value, routes.length - 1)
        selectedRouteIndex.value = index
        mapRef.value?.displayRoutes(routes, routes[index])
        mapRef.value?.setMarkers(routeRequest.value.origin, routeRequest.value.destination, routeRequest.value.waypoints)
    } else if (routeRequest.value) {
        mapRef.value?.setMarkers(routeRequest.value.origin, routeRequest.value.destination, routeRequest.value.waypoints)

        if (props.editingTransport?.polyline) {
            const existingRoute = { polyline: props.editingTransport.polyline }
            mapRef.value?.displayRoutes([existingRoute], existingRoute)
        }
    }
}

async function saveRoute() {
    if (!routeRequest.value || !effectiveRoute.value || driverId.value === undefined || tractorId.value === null || isSaving.value) {
        return
    }

    try {
        isSaving.value = true
        const sourceWindows = serializeWindows(serviceWindows)
        if ((sourceWindows.departureStart && sourceWindows.departureEnd && sourceWindows.departureStart > sourceWindows.departureEnd)
            || (sourceWindows.arrivalStart && sourceWindows.arrivalEnd && sourceWindows.arrivalStart > sourceWindows.arrivalEnd)) {
            throw new Error('Une borne minimale ne peut pas dépasser la borne maximale.')
        }
        if ([loadingDurationMinutes.value, unloadingDurationMinutes.value]
            .some(value => value !== '' && (!Number.isInteger(value) || value < 0))) {
            throw new Error('Les durées doivent être des nombres entiers positifs ou nuls.')
        }
        if (sourceWindows.arrivalStart && (routeRequest.value.timeMode !== 'ARRIVAL'
            || new Date(routeRequest.value.routeTime ?? '').getTime() !== new Date(sourceWindows.arrivalStart).getTime())) {
            const updatedRequest: RouteRequest = {
                ...routeRequest.value,
                routeTime: sourceWindows.arrivalStart,
                timeMode: 'ARRIVAL',
                tractorId: tractorId.value,
                semiTrailerId: semiTrailerId.value ?? undefined,
                driverId: driverId.value,
                emptyTrip: emptyTrip.value,
            }
            const response: RouteResponse = await calculateRoute(updatedRequest)
            const index = response.routes.findIndex(route => route.type === effectiveRoute.value?.type)
            if (!response.routes.length) throw new Error('Aucun itinéraire disponible.')
            handleRouteCalculated({ response, request: updatedRequest })
            selectedRouteIndex.value = index < 0 ? 0 : index
        }
        const savedRoute = effectiveRoute.value
        if (!savedRoute) throw new Error('Aucun itinéraire disponible.')
        const request: CreateTransportFromRouteRequest = {
            transport: {
                name: transportTitle.value,
                customerId: customerId.value,
                driverId: driverId.value,
                tractorId: tractorId.value,
                semiTrailerId: semiTrailerId.value ?? undefined,
                emptyTrip: emptyTrip.value,
                routeTime: routeRequest.value.routeTime,
                timeMode: routeRequest.value.timeMode,
                originName: routeRequest.value.origin.name,
                originAddress: routeRequest.value.origin.address,
                originLat: routeRequest.value.origin.lat,
                originLng: routeRequest.value.origin.lng,
                destinationName: routeRequest.value.destination.name,
                destinationAddress: routeRequest.value.destination.address,
                destinationLat: routeRequest.value.destination.lat,
                destinationLng: routeRequest.value.destination.lng,
                revenue: revenue.value,
                serviceTiming: {
                    ...sourceWindows,
                    loadingDurationMinutes: loadingDurationMinutes.value === '' ? null : loadingDurationMinutes.value,
                    unloadingDurationMinutes: unloadingDurationMinutes.value === '' ? null : unloadingDurationMinutes.value,
                },
                waypoints: (routeRequest.value.waypoints ?? [])
                    .filter(point => point.eventId == null)
                    .map(({ lat, lng, name, address }) => ({ lat, lng, name, address })),
            },
            selectedRoute: savedRoute,
        }

        if (props.editingTransport) {
            await updateTransportFromRoute(props.editingTransport.id, request)
            notification.success('Planning mis à jour', `Le transport « ${transportTitle.value} » a bien été modifié.`)
        } else {
            await createTransportFromRoute(request)
            notification.success('Planning enregistré', `Le transport « ${transportTitle.value} » a bien été ajouté.`)
        }

        emit('saved')
    } catch (error) {
        notification.error('Enregistrement impossible', error instanceof Error && !('response' in error)
            ? error.message : getApiErrorMessage(error, 'Le transport n’a pas pu être enregistré.'))
    } finally {
        isSaving.value = false
    }
}

watch(() => props.show, (show) => {
    if (!show) return

    driverId.value = undefined
    customerId.value = undefined
    revenue.value = null
    void initializeFromProps()
})

watch(driverId, async (id) => {
    if (id === undefined) return

    const driver = await getDriver(id)
    tractorId.value = driver.tractorId
    semiTrailerId.value = driver.semiTrailerId
})

onMounted(async () => {
    const [loadedDrivers, loadedCustomers, loadedTractors, loadedSemiTrailers] = await Promise.all([
        getDrivers(),
        getCustomers(),
        getTractors(),
        getSemiTrailers(),
    ])
    drivers.value = loadedDrivers
    customers.value = loadedCustomers
    tractors.value = loadedTractors
    semiTrailers.value = loadedSemiTrailers
})
</script>

<template>
    <AppModal :show="show" panel-class="max-w-screen-2xl" @close="close">
        <div class="flex h-[82vh] max-h-[720px] flex-col">
            <!-- HEADER -->
            <div class="flex shrink-0 items-start justify-between gap-4 pb-4">
                <h2 class="text-xl font-bold text-slate-900">{{ props.editingTransport ? "Modifier l'itinéraire" :
                    'Créer et assigner un itinéraire' }}</h2>
                <button type="button"
                    class="rounded-full p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                    aria-label="Fermer" @click="close">
                    <X :size="20" />
                </button>
            </div>

            <!-- BODY -->
            <div class="flex min-h-0 flex-1 flex-col gap-5 overflow-hidden">

                <!-- TOP ROW : ASSIGNMENT & SEARCH -->
                <div class="grid min-h-0 flex-1 gap-5 lg:grid-cols-3">

                    <!-- COLUMN 1 : ASSIGNMENT -->
                    <section class="min-h-0 pr-1">
                        <div
                            class="space-y-4 rounded-2xl border border-slate-200 bg-slate-50/60 p-4 h-full overflow-auto">
                            <h3 class="flex items-center gap-2 text-sm font-semibold text-slate-700">
                                <ListChecks :size="16" class="text-slate-400" />
                                Affectation
                            </h3>

                            <div class="space-y-1.5">
                                <label
                                    class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    <User :size="14" />
                                    Chauffeur
                                </label>
                                <select v-model="driverId"
                                    class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20">
                                    <option :value="undefined">Sélectionner un chauffeur</option>
                                    <option v-for="driver in drivers" :key="driver.id" :value="driver.id">{{
                                        driver.firstName }} {{ driver.lastName }}</option>
                                </select>
                            </div>

                            <div class="space-y-1.5">
                                <label
                                    class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    <Truck :size="14" />
                                    Tracteur
                                </label>
                                <select v-model="tractorId"
                                    class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20">
                                    <option :value="null">Sélectionner un tracteur</option>
                                    <option v-for="tractor in tractors" :key="tractor.id" :value="tractor.id">
                                        {{ tractor.registration }}
                                    </option>
                                </select>
                            </div>

                            <div class="space-y-1.5">
                                <label
                                    class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    <Truck :size="14" />
                                    Semi-remorque
                                </label>
                                <select v-model="semiTrailerId"
                                    class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20">
                                    <option :value="null">Sélectionner une semi-remorque</option>
                                    <option v-for="semiTrailer in semiTrailers" :key="semiTrailer.id"
                                        :value="semiTrailer.id">
                                        {{ semiTrailer.registration }}
                                    </option>
                                </select>
                            </div>

                            <div class="space-y-1.5">
                                <label
                                    class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    <Building2 :size="14" />
                                    Donneur d’ordre
                                </label>
                                <select v-model="customerId"
                                    class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20">
                                    <option :value="undefined">Aucun donneur d’ordre</option>
                                    <option v-for="customer in customers" :key="customer.id" :value="customer.id">{{
                                        customer.name }}</option>
                                </select>
                            </div>

                            <label
                                class="flex cursor-pointer items-center justify-between rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 shadow-sm">
                                <span class="flex items-center gap-2">
                                    <PackageX :size="16" class="text-slate-400" />
                                    Trajet à vide
                                </span>
                                <span class="relative inline-flex h-5 w-9 shrink-0 items-center">
                                    <input v-model="emptyTrip" type="checkbox" class="peer sr-only" />
                                    <span
                                        class="absolute inset-0 rounded-full bg-slate-200 transition peer-checked:bg-blue-600"></span>
                                    <span
                                        class="absolute left-0.5 h-4 w-4 rounded-full bg-white shadow transition peer-checked:translate-x-4"></span>
                                </span>
                            </label>

                            <div>
                                <label
                                    class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    <Euro :size="14" />
                                    Revenu (€)
                                </label>

                                <input v-model.number="revenue" type="number" placeholder="0" min="0" step="0.01"
                                    class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20" />
                            </div>
                            <fieldset class="space-y-2">
                                <legend class="text-sm font-semibold text-slate-700">Durées du service (minutes)</legend>
                                <label class="block text-xs text-slate-500">
                                    Chargement
                                    <input v-model.number="loadingDurationMinutes" type="number" min="0" step="1"
                                        class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm" />
                                </label>
                                <label class="block text-xs text-slate-500">
                                    Déchargement
                                    <input v-model.number="unloadingDurationMinutes" type="number" min="0" step="1"
                                        class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm" />
                                </label>
                            </fieldset>
                            <fieldset class="space-y-2">
                                <legend class="text-sm font-semibold text-slate-700">Fenêtres sources du service (optionnelles)</legend>
                                <label v-for="field in windowFields" :key="field.key" class="block text-xs text-slate-500">
                                    {{ field.label }}
                                    <input v-model="serviceWindows[field.key]" type="datetime-local" step="1"
                                        class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm" />
                                </label>
                            </fieldset>
                        </div>
                    </section>

                    <!-- COLUMN 2 : SEARCH -->
                    <section class="flex min-h-0 flex-col pr-1">
                        <div class="min-h-0 flex-1 rounded-2xl border border-slate-200 p-4 flex flex-col">
                            <h3 class="mb-4 flex items-center gap-2 text-sm font-semibold text-slate-700">
                                <RouteIcon :size="16" class="text-slate-400" />
                                Recherche d'itinéraire
                            </h3>
                            <div class="custom-scrollbar min-h-0 flex-1 overflow-auto pr-2">
                                <RouteForm :initial-request="routeRequest" v-model:tractor-id="tractorId"
                                    v-model:semi-trailer-id="semiTrailerId" v-model:driver-id="driverId"
                                    v-model:empty-trip="emptyTrip" @route-calculated="handleRouteCalculated" />
                            </div>
                        </div>

                        <div v-if="routeResponse?.routes?.length" class="mt-4 space-y-2">
                            <button v-for="(route, index) in routeResponse.routes" :key="index" type="button"
                                class="w-full rounded-xl border p-3 text-left transition"
                                :class="index === selectedRouteIndex ? 'border-blue-500 bg-blue-50' : 'border-slate-200 hover:bg-slate-50'"
                                @click="selectRoute(index)">
                                <span
                                    class="rounded-md bg-emerald-100 px-2 py-1 text-xs font-semibold text-emerald-700">
                                    {{ route.type === 'CHEAPEST' ? 'Route la plus économique' : 'Route la plus rapide'
                                    }}
                                </span>
                                <div class="mt-2 flex items-center justify-between gap-3">
                                    <span class="font-semibold">{{ formatCurrency(route.costs.totalCost) }}</span>
                                    <span class="text-sm font-semibold">{{ formatDurationSeconds(route.duration)
                                    }}</span>
                                </div>
                                <div class="mt-1 text-xs text-slate-500">{{ formatDistance(route.distanceMeters) }}
                                </div>
                            </button>
                        </div>
                    </section>
                    <!-- MAP & SUMMARY -->
                    <section class="flex min-h-0 min-w-0 flex-col gap-4">
                        <div
                            class="min-h-0 flex-1 overflow-hidden rounded-2xl border border-slate-200 bg-slate-100 shadow-inner">
                            <HereMap ref="mapRef" @route-selected="selectRoute" />
                        </div>

                        <div class="shrink-0 rounded-2xl border border-slate-200 bg-slate-50/60 p-4 text-sm">
                            <template v-if="effectiveRoute && routeRequest">
                                <div class="grid grid-cols-2 gap-3">
                                    <div class="flex items-center gap-2">
                                        <MapPinned :size="16" class="shrink-0 text-slate-400" />
                                        <div>
                                            <p class="text-slate-500">Distance</p>
                                            <p class="font-semibold">{{ formatDistance(effectiveRoute.distanceMeters) }}
                                            </p>
                                        </div>
                                    </div>

                                    <div class="flex items-center gap-2">
                                        <Clock :size="16" class="shrink-0 text-slate-400" />
                                        <div>
                                            <p class="text-slate-500">Durée</p>
                                            <p class="font-semibold">{{ formatDurationSeconds(effectiveRoute.duration)
                                            }}
                                            </p>
                                        </div>
                                    </div>

                                    <div class="flex items-center gap-2">
                                        <Fuel :size="16" class="shrink-0 text-slate-400" />
                                        <div>
                                            <p class="text-slate-500">Carburant</p>
                                            <p class="font-semibold">{{ formatCurrency(effectiveRoute.costs.fuelCost) }}
                                            </p>
                                        </div>
                                    </div>

                                    <div class="flex items-center gap-2">
                                        <Receipt :size="16" class="shrink-0 text-slate-400" />
                                        <div>
                                            <p class="text-slate-500">Péage</p>
                                            <p class="font-semibold">{{ formatCurrency(effectiveRoute.costs.tollCost) }}
                                            </p>
                                        </div>
                                    </div>

                                    <div class="flex items-center gap-2">
                                        <Wallet :size="16" class="shrink-0 text-slate-400" />
                                        <div>
                                            <p class="text-slate-500">Coût total</p>
                                            <p class="font-semibold">{{ formatCurrency(effectiveRoute.costs.totalCost)
                                            }}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </template>

                            <p v-else class="text-sm text-slate-500">
                                Rechercher un itinéraire pour l'attribuer
                            </p>
                        </div>
                    </section>
                </div>
            </div>

            <!-- FOOTER -->
            <div class="mt-5 flex shrink-0 flex-wrap items-center justify-between gap-3 border-t border-slate-300 pt-4">
                <div v-if="routeRequest && effectiveRoute" class="flex flex-wrap items-center gap-5 text-sm">
                    <div class="flex items-center gap-2">
                        <span class="text-slate-400">Départ</span>
                        <span class="font-semibold text-slate-700">
                            {{ formatDateTime(plannedStart) }}
                        </span>
                    </div>

                    <div class="h-4 w-px bg-slate-200"></div>

                    <div class="flex items-center gap-2">
                        <span class="text-slate-400">Arrivée</span>
                        <span class="font-semibold text-slate-700">
                            {{ formatDateTime(plannedEnd) }}
                        </span>
                    </div>
                </div>

                <div class="ml-auto flex gap-3">
                    <button type="button"
                        class="rounded-xl border border-slate-300 px-4 py-2 text-slate-700 hover:bg-slate-50"
                        :disabled="isSaving" @click="close">
                        Annuler
                    </button>

                    <button type="button"
                        class="rounded-xl bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                        :disabled="!effectiveRoute || !routeRequest || driverId === undefined || tractorId === null || isSaving"
                        @click="saveRoute">
                        {{ isSaving ? 'Enregistrement...' : (props.editingTransport ? "Mettre à jour l'itinéraire" :
                            "Valider l'itinéraire") }}
                    </button>
                </div>
            </div>

        </div>
    </AppModal>
</template>
