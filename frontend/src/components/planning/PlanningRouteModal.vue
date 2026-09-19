<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'

import AppModal from '@/components/ui/AppModal.vue'
import HereMap from '@/components/maps/HereMap.vue'
import RouteForm from '@/components/maps/RouteForm.vue'

import { createTransportFromRoute } from '@/api/transportApi'
import { getCustomers } from '@/api/customerApi'
import { getDriver, getDrivers } from '@/api/driver/driverApi'
import { getTractors } from '@/api/vehicle/tractorApi'
import { getSemiTrailers } from '@/api/vehicle/semiTrailerApi'
import { getApiErrorMessage } from '@/api/utils'
import { useNotification } from '@/composables/useNotification'

import type { Customer } from '@/models/Customer'
import type { DriverSummary } from '@/models/driver/Driver'
import type { TractorSummary } from '@/models/vehicle/Tractor'
import type { SemiTrailerSummary } from '@/models/vehicle/SemiTrailer'
import type { RouteRequest, RouteResponse } from '@/models/route/Route'
import type { CreateTransportFromRouteRequest } from '@/models/transport/TransportRequest'

import { formatCurrency, formatDateTime, formatDistance, formatDurationSeconds } from '@/utils/formatters'
import { formatVehicleLabel } from '@/utils/vehicleUtils'

import {
    Building2,
    Clock,
    Fuel,
    Landmark,
    ListChecks,
    MapPinned,
    PackageX,
    Route as RouteIcon,
    Truck,
    User,
    Wallet,
    X,
    Euro
} from 'lucide-vue-next'

const props = defineProps<{
    show: boolean
    initialRouteRequest?: RouteRequest | null
    initialRouteResponse?: RouteResponse | null
    initialSelectedRouteIndex?: number
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
const customers = ref<Customer[]>([])
const tractors = ref<TractorSummary[]>([])
const semiTrailers = ref<SemiTrailerSummary[]>([])
const isSaving = ref(false)

const selectedRoute = computed(() => routeResponse.value?.routes[selectedRouteIndex.value])

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
    if (routeRequest.value.timeMode === 'ARRIVAL' && selectedRoute.value) {
        value.setSeconds(value.getSeconds() - selectedRoute.value.duration)
    }

    return value.toISOString()
})

const plannedEnd = computed(() => {
    if (!routeRequest.value?.routeTime) return ''
    if (routeRequest.value.timeMode === 'ARRIVAL') return routeRequest.value.routeTime

    const value = new Date(routeRequest.value.routeTime)
    value.setSeconds(value.getSeconds() + (selectedRoute.value?.duration ?? 0))
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
    selectedRouteIndex.value = index
    if (routeResponse.value) {
        mapRef.value?.displayRoutes(routeResponse.value.routes, routeResponse.value.routes[index])
    }
}

async function initializeFromProps() {
    routeRequest.value = props.initialRouteRequest ?? null
    routeResponse.value = props.initialRouteResponse ?? null
    selectedRouteIndex.value = props.initialSelectedRouteIndex ?? 0
    tractorId.value = props.initialRouteRequest?.tractorId ?? null
    semiTrailerId.value = props.initialRouteRequest?.semiTrailerId ?? null
    emptyTrip.value = props.initialRouteRequest?.emptyTrip ?? false

    await nextTick()

    if (routeRequest.value && routeResponse.value?.routes.length) {
        const routes = routeResponse.value.routes
        const index = Math.min(selectedRouteIndex.value, routes.length - 1)
        selectedRouteIndex.value = index
        mapRef.value?.displayRoutes(routes, routes[index])
        mapRef.value?.setMarkers(routeRequest.value.origin, routeRequest.value.destination, routeRequest.value.waypoints)
    }
}

async function saveRoute() {
    if (!routeRequest.value || !selectedRoute.value || driverId.value === undefined || isSaving.value) {
        return
    }

    const request: CreateTransportFromRouteRequest = {
        transport: {
            name: transportTitle.value,
            customerId: customerId.value,
            driverId: driverId.value,
            tractorId: routeRequest.value.tractorId,
            semiTrailerId: routeRequest.value.semiTrailerId,
            emptyTrip: routeRequest.value.emptyTrip,
            plannedStart: plannedStart.value,
            plannedEnd: plannedEnd.value,
            originName: routeRequest.value.origin.name,
            originAddress: routeRequest.value.origin.address,
            originLat: routeRequest.value.origin.lat,
            originLng: routeRequest.value.origin.lng,
            destinationName: routeRequest.value.destination.name,
            destinationAddress: routeRequest.value.destination.address,
            destinationLat: routeRequest.value.destination.lat,
            destinationLng: routeRequest.value.destination.lng,
            revenue: revenue.value,
        },
        selectedRoute: selectedRoute.value,
    }

    try {
        isSaving.value = true
        await createTransportFromRoute(request)
        notification.success('Planning enregistré', `Le transport « ${transportTitle.value} » a bien été ajouté.`)
        emit('saved')
    } catch (error) {
        notification.error('Enregistrement impossible', getApiErrorMessage(error, 'Le transport n’a pas pu être ajouté au planning.'))
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
    <AppModal :show="show" panel-class="max-w-7xl" @close="close">
        <div class="flex h-[90vh] max-h-[90vh] flex-col">
            <!-- HEADER -->
            <div class="flex shrink-0 items-start justify-between gap-4 border-b border-slate-100 pb-4">
                <h2 class="text-xl font-bold text-slate-900">Créer et assigner un itinéraire</h2>
                <button type="button"
                    class="rounded-full p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                    aria-label="Fermer" @click="close">
                    <X :size="20" />
                </button>
            </div>

            <!-- BODY -->
            <div class="flex min-h-0 flex-1 flex-col gap-5 overflow-hidden">

                <!-- TOP ROW : ASSIGNMENT & SEARCH -->
                <div class="grid h-[520px] min-h-0 gap-5 lg:grid-cols-2">

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

                                <input v-model.number="revenue" type="number" min="0" step="0.01"
                                    class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20" />
                            </div>
                        </div>
                    </section>

                    <!-- COLUMN 2 : SEARCH -->
                    <section class="min-h-0 pr-1">
                        <div class="rounded-2xl border border-slate-200 p-4 h-full flex flex-col">
                            <h3 class="mb-4 flex items-center gap-2 text-sm font-semibold text-slate-700">
                                <RouteIcon :size="16" class="text-slate-400" />
                                Recherche d'itinéraire
                            </h3>
                            <div class="flex-1 overflow-auto">
                                <RouteForm :initial-request="props.initialRouteRequest" v-model:tractor-id="tractorId"
                                    v-model:semi-trailer-id="semiTrailerId" v-model:empty-trip="emptyTrip"
                                    @route-calculated="handleRouteCalculated" />
                            </div>
                        </div>

                        <!-- <div v-if="routeResponse?.routes?.length" class="rounded-2xl border border-slate-200 p-4">
                            <h3 class="mb-3 flex items-center justify-between text-sm font-semibold text-slate-700">
                                <span>Itinéraires trouvés</span>
                                <span
                                    class="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-500">{{
                                        routeResponse.routes.length }}</span>
                            </h3>
                            <div class="space-y-2">
                                <button v-for="(route, index) in routeResponse.routes" :key="index" type="button"
                                    class="w-full rounded-xl border p-3 text-left transition"
                                    :class="index === selectedRouteIndex ? 'border-blue-500 bg-blue-50' : 'border-slate-200 hover:bg-slate-50'"
                                    @click="selectRoute(Number(index))">
                                    <div class="flex items-center justify-between gap-3">
                                        <span class="font-semibold">Option {{ Number(index) + 1 }}</span>
                                        <span class="text-sm font-semibold">{{ formatCurrency(route.costs.totalCost)
                                        }}</span>
                                    </div>
                                    <div class="mt-1 text-xs text-slate-500">{{ formatDistance(route.distanceMeters) }}
                                        ·
                                        {{
                                            formatDurationSeconds(route.duration) }}</div>
                                </button>
                            </div>
                        </div>-->
                    </section>
                </div>

                <!-- MAP & SUMMARY -->
                <section class="flex min-h-0 min-w-0 flex-1 flex-col gap-4">
                    <div
                        class="min-h-0 flex-1 overflow-hidden rounded-2xl border border-slate-200 bg-slate-100 shadow-inner">
                        <HereMap ref="mapRef" />
                    </div>

                    <div class="shrink-0 rounded-2xl border border-slate-200 bg-slate-50/60 p-4 text-sm">
                        <template v-if="selectedRoute && routeRequest">
                            <div class="grid grid-cols-2 gap-3 sm:grid-cols-5">
                                <div class="flex items-center gap-2">
                                    <MapPinned :size="16" class="shrink-0 text-slate-400" />
                                    <div>
                                        <p class="text-slate-500">Distance</p>
                                        <p class="font-semibold">{{ formatDistance(selectedRoute.distanceMeters) }}</p>
                                    </div>
                                </div>

                                <div class="flex items-center gap-2">
                                    <Clock :size="16" class="shrink-0 text-slate-400" />
                                    <div>
                                        <p class="text-slate-500">Durée</p>
                                        <p class="font-semibold">{{ formatDurationSeconds(selectedRoute.duration) }}</p>
                                    </div>
                                </div>

                                <div class="flex items-center gap-2">
                                    <Fuel :size="16" class="shrink-0 text-slate-400" />
                                    <div>
                                        <p class="text-slate-500">Carburant</p>
                                        <p class="font-semibold">{{ formatCurrency(selectedRoute.costs.fuelCost) }}</p>
                                    </div>
                                </div>

                                <div class="flex items-center gap-2">
                                    <Landmark :size="16" class="shrink-0 text-slate-400" />
                                    <div>
                                        <p class="text-slate-500">Péage</p>
                                        <p class="font-semibold">{{ formatCurrency(selectedRoute.costs.tollCost) }}</p>
                                    </div>
                                </div>

                                <div class="flex items-center gap-2">
                                    <Wallet :size="16" class="shrink-0 text-slate-400" />
                                    <div>
                                        <p class="text-slate-500">Coût total</p>
                                        <p class="font-semibold">{{ formatCurrency(selectedRoute.costs.totalCost) }}</p>
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

            <!-- FOOTER -->
            <div class="mt-5 flex shrink-0 flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
                <div v-if="routeRequest && selectedRoute" class="flex flex-wrap gap-4 text-xs text-slate-500">
                    <span>Départ : {{ formatDateTime(plannedStart) }}</span>
                    <span>Arrivée : {{ formatDateTime(plannedEnd) }}</span>
                </div>
                <div class="ml-auto flex gap-3">
                    <button type="button"
                        class="rounded-xl border border-slate-300 px-4 py-2 text-slate-700 hover:bg-slate-50"
                        :disabled="isSaving" @click="close">Annuler</button>
                    <button type="button"
                        class="rounded-xl bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                        :disabled="!selectedRoute || !routeRequest || driverId === undefined || isSaving"
                        @click="saveRoute">
                        {{ isSaving ? 'Enregistrement...' : "Valider l'itinéraire" }}
                    </button>
                </div>
            </div>
        </div>
    </AppModal>
</template>
