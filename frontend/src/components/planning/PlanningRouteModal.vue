<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'

import AppModal from '@/components/ui/AppModal.vue'
import HereMap from '@/components/maps/HereMap.vue'
import RouteForm from '@/components/maps/RouteForm.vue'

import { createTransportFromRoute } from '@/api/transportApi'
import { getCustomers } from '@/api/customerApi'
import { getDrivers } from '@/api/driver/driverApi'
import { getApiErrorMessage } from '@/api/utils'
import { useNotification } from '@/composables/useNotification'

import type { Customer } from '@/models/Customer'
import type { DriverSummary } from '@/models/driver/Driver'
import type { RouteRequest, RouteResponse } from '@/models/route/Route'
import type { CreateTransportFromRouteRequest } from '@/models/transport/TransportRequest'

import { formatCurrency, formatDateTime, formatDistance, formatDurationSeconds } from '@/utils/formatters'

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
const drivers = ref<DriverSummary[]>([])
const customers = ref<Customer[]>([])
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
    void initializeFromProps()
})

onMounted(async () => {
    const [loadedDrivers, loadedCustomers] = await Promise.all([getDrivers(), getCustomers()])
    drivers.value = loadedDrivers
    customers.value = loadedCustomers
})
</script>

<template>
    <AppModal :show="show" panel-class="max-w-6xl" @close="close">
        <div class="flex max-h-[90vh] flex-col">
            <div class="mb-5 flex items-start justify-between gap-4">
                <div>
                    <h2 class="text-xl font-bold text-slate-900">Créer et assigner un itinéraire</h2>
                    <p class="mt-1 text-sm text-slate-500">Recherchez un trajet, sélectionnez-le puis assignez-le au
                        planning.</p>
                </div>
                <button type="button" class="text-2xl leading-none text-slate-400 hover:text-slate-700"
                    aria-label="Fermer" @click="close">×</button>
            </div>

            <div class="grid min-h-0 flex-1 gap-6 overflow-hidden lg:grid-cols-[360px_minmax(0,1fr)]">
                <section class="min-h-0 space-y-4 overflow-y-auto pr-2">
                    <RouteForm :initial-request="props.initialRouteRequest" @route-calculated="handleRouteCalculated" />

                    <div v-if="routeResponse?.routes?.length" class="space-y-2 border-t border-slate-200 pt-4">
                        <h3 class="font-semibold text-slate-800">Itinéraires trouvés</h3>
                        <button v-for="(route, index) in routeResponse.routes" :key="index" type="button"
                            class="w-full rounded-xl border p-3 text-left transition"
                            :class="index === selectedRouteIndex ? 'border-blue-500 bg-blue-50' : 'border-slate-200 hover:bg-slate-50'"
                            @click="selectRoute(Number(index))">
                            <div class="flex items-center justify-between gap-3">
                                <span class="font-semibold">Option {{ Number(index) + 1 }}</span>
                                <span class="text-sm font-semibold">{{ formatCurrency(route.costs.totalCost) }}</span>
                            </div>
                            <div class="mt-1 text-xs text-slate-500">{{ formatDistance(route.distanceMeters) }} · {{
                                formatDurationSeconds(route.duration) }}</div>
                        </button>
                    </div>
                </section>

                <section class="flex min-h-0 min-w-0 flex-col gap-4">
                    <div class="min-h-[300px] flex-1 overflow-hidden rounded-xl border border-slate-200 bg-slate-100">
                        <HereMap ref="mapRef" />
                    </div>

                    <div
                        class="grid shrink-0 grid-cols-2 gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm sm:grid-cols-5">
                        <template v-if="selectedRoute && routeRequest">
                            <div>
                                <p class="text-slate-500">Distance</p>
                                <p class="font-semibold">{{ formatDistance(selectedRoute.distanceMeters) }}</p>
                            </div>
                            <div>
                                <p class="text-slate-500">Durée</p>
                                <p class="font-semibold">{{ formatDurationSeconds(selectedRoute.duration) }}</p>
                            </div>
                            <div>
                                <p class="text-slate-500">Carburant</p>
                                <p class="font-semibold">{{ formatCurrency(selectedRoute.costs.fuelCost) }}</p>
                            </div>
                            <div>
                                <p class="text-slate-500">Péage</p>
                                <p class="font-semibold">{{ formatCurrency(selectedRoute.costs.tollCost) }}</p>
                            </div>
                            <div>
                                <p class="text-slate-500">Coût total</p>
                                <p class="font-semibold">{{ formatCurrency(selectedRoute.costs.totalCost) }}</p>
                            </div>
                        </template>
                        <p v-else class="col-span-2 text-sm text-slate-500 sm:col-span-4">
                            Rechercher un itinéraire pour l'attribuer
                        </p>
                    </div>
                </section>
            </div>

            <div class="mt-5 grid gap-4 border-t border-slate-200 pt-5 lg:grid-cols-[1fr_1fr_1fr]">
                <select v-model="driverId" class="rounded-lg border border-slate-300 p-3">
                    <option :value="undefined">Sélectionner un chauffeur</option>
                    <option v-for="driver in drivers" :key="driver.id" :value="driver.id">{{ driver.firstName }} {{
                        driver.lastName }}</option>
                </select>
                <select v-model="customerId" class="rounded-lg border border-slate-300 p-3 lg:col-span-2">
                    <option :value="undefined">Aucun donneur d’ordre</option>
                    <option v-for="customer in customers" :key="customer.id" :value="customer.id">{{ customer.name }}
                    </option>
                </select>
            </div>

            <div v-if="routeRequest && selectedRoute" class="mt-3 flex flex-wrap gap-4 text-xs text-slate-500">
                <span>Départ : {{ formatDateTime(plannedStart) }}</span>
                <span>Arrivée : {{ formatDateTime(plannedEnd) }}</span>
            </div>

            <div class="mt-5 flex justify-end gap-3">
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
    </AppModal>
</template>
