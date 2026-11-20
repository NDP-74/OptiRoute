<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'

import AppModal from '@/components/ui/AppModal.vue'
import ConfirmDeleteModal from '@/components/ui/ConfirmDeleteModal.vue'
import CreateSelect from '@/components/ui/CreateSelect.vue'
import TractorModal from '@/components/vehicles/TractorModal.vue'
import SemiTrailerModal from '@/components/vehicles/SemiTrailerModal.vue'
import DriverModal from '@/components/drivers/DriverModal.vue'
import CustomerModal from '@/components/customers/CustomerModal.vue'
import HereMap from '@/components/maps/HereMap.vue'
import RouteForm from '@/components/maps/RouteForm.vue'

import { createTransportFromRoute, updateTransportFromRoute } from '@/api/transportApi'
import { calculateRoute } from '@/api/here/mapsApi'
import { deleteEvent } from '@/api/eventApi'
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
import type { RouteDto, RouteRequest, RouteResponse, RouteWaypoint } from '@/models/route/Route'
import type { CreateTransportFromRouteRequest, TimeWindows } from '@/models/transport/TransportRequest'
import type { TransportDetail } from '@/models/transport/TransportDetail'
import type { EventResponse } from '@/models/Event'

import { formatCurrency, formatDateTime, formatDistance, formatDurationSeconds, formatParisDateTimeLocal, parisDateTimeLocalToIso } from '@/utils/formatters'
import { formatVehicleRegistration } from '@/utils/vehicleUtils'

import {
    Building2,
    CalendarClock,
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
    ArrowLeft,
} from 'lucide-vue-next'

const props = defineProps<{
    show: boolean
    initialRouteRequest?: RouteRequest | null
    initialRouteResponse?: RouteResponse | null
    initialSelectedRouteIndex?: number
    editingTransport?: TransportDetail | null
    events?: EventResponse[]
    // Rendu sans fenêtre propre, à l'intérieur d'une autre modale (replanification depuis la modale de modification)
    embedded?: boolean
}>()

const emit = defineEmits<{
    close: []
    saved: []
}>()

const notification = useNotification()
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
const mapRef = ref<InstanceType<typeof HereMap> | null>(null)
const isDashdocSimulationEnabled = ref(false)
const formSession = ref(0)
const modeStates = reactive({
    manual: { request: null as RouteRequest | null, response: null as RouteResponse | null, selectedIndex: 0, dirty: false },
    dashdoc: { request: null as RouteRequest | null, response: null as RouteResponse | null, selectedIndex: 0, dirty: false },
})
type EntryMode = keyof typeof modeStates
const entryModes = ['manual', 'dashdoc'] as const
const activeMode = computed<EntryMode>(() => isDashdocSimulationEnabled.value ? 'dashdoc' : 'manual')
const activeState = computed(() => modeStates[activeMode.value])
const routeResponse = computed({
    get: () => activeState.value.response,
    set: value => { activeState.value.response = value },
})
const routeRequest = computed({
    get: () => activeState.value.request,
    set: value => { activeState.value.request = value },
})
const selectedRouteIndex = computed({
    get: () => activeState.value.selectedIndex,
    set: value => { activeState.value.selectedIndex = value },
})
const initialFormRequest = ref<RouteRequest | null>(null)
const driverId = ref<number>()
const customerId = ref<number>()
const selectedDriverId = computed({
    get: () => driverId.value ?? null,
    set: (value: number | null) => { driverId.value = value ?? undefined },
})
const selectedCustomerId = computed({
    get: () => customerId.value ?? null,
    set: (value: number | null) => { customerId.value = value ?? undefined },
})
const tractorId = ref<number | null>(null)
const semiTrailerId = ref<number | null>(null)
const emptyTrip = ref(false)
const revenue = ref<number | null>(null)
const drivers = ref<DriverSummary[]>([])
const customers = ref<CustomerSummary[]>([])
const tractors = ref<TractorSummary[]>([])
const semiTrailers = ref<SemiTrailerSummary[]>([])
const showCreateDriver = ref(false)
const showCreateCustomer = ref(false)
const driverOptions = computed(() => drivers.value.map(driver => ({
    value: driver.id,
    label: `${driver.firstName} ${driver.lastName}`,
})))
const customerOptions = computed(() => customers.value.map(customer => ({
    value: customer.id,
    label: customer.name,
})))
const showCreateTractor = ref(false)
const showCreateSemiTrailer = ref(false)
const tractorOptions = computed(() => tractors.value.map(tractor => ({
    value: tractor.id,
    label: formatVehicleRegistration(tractor.registration),
})))
const semiTrailerOptions = computed(() => semiTrailers.value.map(semiTrailer => ({
    value: semiTrailer.id,
    label: formatVehicleRegistration(semiTrailer.registration),
})))

function onTractorCreated(tractor: TractorSummary) {
    tractors.value.push(tractor)
    tractorId.value = tractor.id
    showCreateTractor.value = false
}

function onSemiTrailerCreated(semiTrailer: SemiTrailerSummary) {
    semiTrailers.value.push(semiTrailer)
    semiTrailerId.value = semiTrailer.id
    showCreateSemiTrailer.value = false
}

async function onDriverCreated(driver: DriverSummary) {
    drivers.value.push(driver)
    driverId.value = driver.id
    showCreateDriver.value = false
    void applyDriverVehicles()
    try {
        const [loadedTractors, loadedSemiTrailers] = await Promise.all([getTractors(), getSemiTrailers()])
        tractors.value = loadedTractors
        semiTrailers.value = loadedSemiTrailers
    } catch (error) {
        notification.error('Chargement impossible', getApiErrorMessage(error, 'Les véhicules du chauffeur n’ont pas pu être actualisés.'))
    }
}

function onCustomerCreated(customer: CustomerSummary) {
    customers.value.push(customer)
    customerId.value = customer.id
    showCreateCustomer.value = false
}

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
const simulationTiming = computed(() => ({
    routeTime: serviceWindows.departureStart
        ? parisDateTimeLocalToIso(serviceWindows.departureStart)
        : serviceWindows.arrivalStart ? parisDateTimeLocalToIso(serviceWindows.arrivalStart) : undefined,
    timeMode: !serviceWindows.departureStart && serviceWindows.arrivalStart ? 'ARRIVAL' as const : 'DEPARTURE' as const,
}))

// Les fenêtres horaires sont saisies et affichées en heure de Paris (fuseau métier), quel que soit le navigateur
function localDate(value?: string | null): string {
    if (!value || Number.isNaN(new Date(value).getTime())) return ''
    return formatParisDateTimeLocal(value)
}

function serializeWindows(values: typeof serviceWindows): TimeWindows {
    return Object.fromEntries(windowFields.map(({ key }) => [key, values[key] ? parisDateTimeLocalToIso(values[key]) : null]))
}

const selectedRoute = computed(() => routeResponse.value?.routes[selectedRouteIndex.value])

// Segments du service en cours de modification : la modification porte sur le service entier
const editingRoutes = computed(() => props.editingTransport?.routes ?? [])

function sumOf(values: (number | null | undefined)[]): number | null {
    if (values.some(value => value == null)) return null
    return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}

function findRouteCost(costs: TransportDetail['costs'], label: string): number {
    return costs.vehicle.costs.find(cost => cost.label === label)?.amount ?? 0
}

function servicePolyline(transport: TransportDetail): string | null {
    const polylines = editingRoutes.value.map(route => route.polyline).filter((polyline): polyline is string => Boolean(polyline))
    if (polylines.length <= 1) return polylines[0] ?? transport.polyline
    return JSON.stringify(polylines)
}

// Route de l'estimation existante, utilisée tant qu'aucun nouveau calcul n'a été fait en modification
const fallbackRoute = computed<RouteDto | null>(() => {
    const transport = props.editingTransport
    if (!transport) return null

    const routes = editingRoutes.value
    const distanceMeters = routes.length ? sumOf(routes.map(route => route.distanceMeters)) : transport.distanceMeters
    const duration = routes.length ? sumOf(routes.map(route => route.durationSeconds)) : transport.durationSeconds
    if (distanceMeters == null || duration == null) {
        return null
    }

    const routeCosts = routes.length ? routes.map(route => route.costs) : [transport.costs]
    const fuelCost = routeCosts.reduce((total, costs) => total + findRouteCost(costs, 'Carburant'), 0)
    const tollCost = routeCosts.reduce((total, costs) => total + findRouteCost(costs, 'Péages'), 0)

    return {
        type: transport.routeType ?? undefined,
        duration,
        baseDuration: duration,
        distanceMeters,
        polyline: servicePolyline(transport) ?? undefined,
        costs: { fuelCost, tollCost, totalCost: fuelCost + tollCost },
    }
})

const effectiveRoute = computed<RouteDto | null>(() => activeState.value.dirty ? null
    : selectedRoute.value ?? (props.editingTransport && activeMode.value === initialEditingMode.value ? fallbackRoute.value : null))
const initialEditingMode = ref<EntryMode>('manual')

const transportTitle = computed(() => {
    const customerName = customers.value.find(customer => customer.id === customerId.value)?.name?.trim()

    if (customerName) {
        return customerName
    }

    if (emptyTrip.value) {
        return 'Trajet à vide'
    }

    const originName = routeRequest.value?.origin.name?.trim() || 'Départ'
    const destinationName = routeRequest.value?.destination.name?.trim() || 'Arrivée'
    return `${originName} - ${destinationName}`
})

// Tant que l'estimation existante est utilisée, on affiche les horaires réellement planifiés (coupures de nuit comprises)
const storedSchedule = computed(() => {
    if (!props.editingTransport || !effectiveRoute.value || effectiveRoute.value !== fallbackRoute.value) return null

    const routes = editingRoutes.value
    return {
        start: routes[0]?.departureStart ?? props.editingTransport.departureStart ?? null,
        end: routes[routes.length - 1]?.arrivalStart ?? props.editingTransport.arrivalStart ?? null,
    }
})

const plannedStart = computed(() => {
    if (storedSchedule.value?.start) return storedSchedule.value.start
    if (!routeRequest.value?.routeTime) return ''

    const value = new Date(routeRequest.value.routeTime)
    if (routeRequest.value.timeMode === 'ARRIVAL' && effectiveRoute.value) {
        value.setSeconds(value.getSeconds() - effectiveRoute.value.duration)
    }

    return value.toISOString()
})

const plannedEnd = computed(() => {
    if (storedSchedule.value?.end) return storedSchedule.value.end
    if (!routeRequest.value?.routeTime) return ''
    if (routeRequest.value.timeMode === 'ARRIVAL') return routeRequest.value.routeTime

    const value = new Date(routeRequest.value.routeTime)
    value.setSeconds(value.getSeconds() + (effectiveRoute.value?.duration ?? 0))
    return value.toISOString()
})

function close() {
    if (!isSaving.value) emit('close')
}

function handleRouteCalculated(data: { response: RouteResponse; request: RouteRequest }, mode: EntryMode = activeMode.value) {
    const state = modeStates[mode]
    state.request = data.request
    state.response = data.response
    state.selectedIndex = 0
    state.dirty = false
    if (mode !== activeMode.value) return

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

// Reconstruit la demande du service complet (origine du 1er segment, destination du dernier) à partir de ses segments
function routeRequestFromTransport(transport: TransportDetail): RouteRequest {
    const events = props.events ?? []
    const routes = transport.routes.length ? transport.routes : null
    const first = routes?.[0] ?? transport
    const last = routes?.[routes.length - 1] ?? transport
    const waypoints = routes
        ? [
            ...routes.flatMap(route => [
                ...(route.waypoints ?? []),
                ...eventWaypoints(events.filter(event => event.routeId === route.routeId)),
            ]),
            ...eventWaypoints(events.filter(event => !routes.some(route => route.routeId === event.routeId))),
        ]
        : [...(transport.waypoints ?? []), ...eventWaypoints(events)]
    const departure = first.departureStart ?? undefined
    const arrival = last.arrivalStart ?? undefined

    return {
        origin: {
            name: first.originName,
            address: first.originAddress ?? '',
            lat: first.originLat,
            lng: first.originLng,
        },
        destination: {
            name: last.destinationName,
            address: last.destinationAddress ?? '',
            lat: last.destinationLat,
            lng: last.destinationLng,
        },
        waypoints,
        mode: first.routeType ?? transport.routeType ?? 'FASTEST',
        emptyTrip: transport.emptyTrip,
        routeTime: departure ?? arrival,
        timeMode: departure || !arrival ? 'DEPARTURE' : 'ARRIVAL',
        driverId: transport.driverId ?? undefined,
    }
}

function serviceRevenue(transport: TransportDetail): number | null {
    if (!transport.routes.length) return transport.revenue
    const revenues = transport.routes.map(route => route.revenue).filter((value): value is number => value != null)
    if (!revenues.length) return null
    return Math.round(revenues.reduce((total, value) => total + value, 0) * 100) / 100
}

async function initializeFromProps() {
    for (const mode of entryModes) {
        modeStates[mode] = { request: null, response: null, selectedIndex: 0, dirty: false }
    }
    loadingDurationMinutes.value = props.editingTransport?.serviceTiming?.loadingDurationMinutes ?? ''
    unloadingDurationMinutes.value = props.editingTransport?.serviceTiming?.unloadingDurationMinutes ?? ''
    for (const { key } of windowFields) {
        serviceWindows[key] = localDate(props.editingTransport?.serviceTiming?.[key])
    }
    // En modification, la replanification passe par le mode manuel : un service issu de la simulation Dashdoc redevient manuel
    isDashdocSimulationEnabled.value = false
    initialEditingMode.value = activeMode.value

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
        revenue.value = serviceRevenue(transport)
    } else {
        routeRequest.value = props.initialRouteRequest ?? null
        routeResponse.value = props.initialRouteResponse ?? null
        selectedRouteIndex.value = props.initialSelectedRouteIndex ?? 0
        tractorId.value = props.initialRouteRequest?.tractorId ?? null
        semiTrailerId.value = props.initialRouteRequest?.semiTrailerId ?? null
        emptyTrip.value = props.initialRouteRequest?.emptyTrip ?? false
    }

    initialFormRequest.value = routeRequest.value
    const otherMode = activeMode.value === 'manual' ? 'dashdoc' : 'manual'
    modeStates[otherMode].request = routeRequest.value ? structuredCloneRoute(routeRequest.value) : null
    formSession.value++
    await nextTick()
    modeStates.manual.dirty = false
    modeStates.dashdoc.dirty = false

    if (routeRequest.value && routeResponse.value?.routes.length) {
        const routes = routeResponse.value.routes
        const index = Math.min(selectedRouteIndex.value, routes.length - 1)
        selectedRouteIndex.value = index
        mapRef.value?.displayRoutes(routes, routes[index])
        mapRef.value?.setMarkers(routeRequest.value.origin, routeRequest.value.destination, routeRequest.value.waypoints)
    } else if (routeRequest.value) {
        mapRef.value?.setMarkers(routeRequest.value.origin, routeRequest.value.destination, routeRequest.value.waypoints)

        const existingPolyline = props.editingTransport ? servicePolyline(props.editingTransport) : null
        if (existingPolyline) {
            const existingRoute = { polyline: existingPolyline }
            mapRef.value?.displayRoutes([existingRoute], existingRoute)
        }
    }
}

function structuredCloneRoute(request: RouteRequest): RouteRequest {
    return {
        ...request, origin: { ...request.origin }, destination: { ...request.destination },
        waypoints: request.waypoints?.map(point => ({ ...point }))
    }
}

watch(isDashdocSimulationEnabled, async () => {
    await nextTick()
    if (!props.show) return
    const routes = activeState.value.dirty ? [] : routeResponse.value?.routes ?? []
    mapRef.value?.displayRoutes(routes, routes[selectedRouteIndex.value])
    if (routeRequest.value) {
        mapRef.value?.setMarkers(routeRequest.value.origin, routeRequest.value.destination, routeRequest.value.waypoints)
    }
})

watch(serviceWindows, () => { modeStates.dashdoc.dirty = true }, { flush: 'sync' })

async function saveRoute() {
    if (!routeRequest.value || !effectiveRoute.value || driverId.value === undefined || tractorId.value === null || isSaving.value) {
        return
    }

    try {
        isSaving.value = true
        const sourceWindows = isDashdocSimulationEnabled.value
            ? serializeWindows(serviceWindows)
            : { departureStart: null, departureEnd: null, arrivalStart: null, arrivalEnd: null }
        if ((sourceWindows.departureStart && sourceWindows.departureEnd && sourceWindows.departureStart > sourceWindows.departureEnd)
            || (sourceWindows.arrivalStart && sourceWindows.arrivalEnd && sourceWindows.arrivalStart > sourceWindows.arrivalEnd)) {
            throw new Error('Une borne minimale ne peut pas dépasser la borne maximale.')
        }
        if ([loadingDurationMinutes.value, unloadingDurationMinutes.value]
            .some(value => value !== '' && (!Number.isInteger(value) || value < 0))) {
            throw new Error('Les durées doivent être des nombres entiers positifs ou nuls.')
        }
        const simulationTime = sourceWindows.departureStart ?? sourceWindows.arrivalStart
        const simulationMode = sourceWindows.departureStart ? 'DEPARTURE' : 'ARRIVAL'
        if (simulationTime && (routeRequest.value.timeMode !== simulationMode
            || new Date(routeRequest.value.routeTime ?? '').getTime() !== new Date(simulationTime).getTime())) {
            const updatedRequest: RouteRequest = {
                ...routeRequest.value,
                routeTime: simulationTime,
                timeMode: simulationMode,
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
                    .map(({ lat, lng, name, address, eventId }) => ({ lat, lng, name, address, eventId })),
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

function openForm() {
    driverId.value = undefined
    customerId.value = undefined
    revenue.value = null
    void initializeFromProps()
}

watch(() => props.show, (show) => {
    if (show) openForm()
})

// Appelé uniquement sur un choix utilisateur : à l'ouverture d'une replanification, le tracteur et la
// semi-remorque enregistrés sur le service ne doivent pas être remplacés par ceux du chauffeur
async function applyDriverVehicles() {
    const id = driverId.value
    if (id === undefined) return

    try {
        const driver = await getDriver(id)
        if (driverId.value !== id) return
        tractorId.value = driver.tractorId
        semiTrailerId.value = driver.semiTrailerId
    } catch (error) {
        notification.error('Chargement impossible', getApiErrorMessage(error, 'Le véhicule du chauffeur n’a pas pu être chargé.'))
    }
}

onMounted(async () => {
    // Monté déjà ouvert (mode intégré) : le watch sur show ne se déclenche pas
    if (props.show) openForm()

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
    <component :is="embedded ? 'div' : AppModal" v-bind="embedded ? {} : { show, panelClass: 'max-w-screen-2xl' }"
        @close="close">
        <div class="flex h-[82vh] max-h-[720px] flex-col">
            <!-- HEADER -->
            <div class="flex shrink-0 items-start justify-between gap-4 pb-4">
                <div class="flex items-center gap-3">
                    <button v-if="embedded" type="button" :disabled="isSaving"
                        class="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 px-2.5 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                        @click="close">
                        <ArrowLeft :size="16" /> Retour
                    </button>
                    <h2 class="text-xl font-bold text-slate-900">{{ props.editingTransport ? 'Replanifier le service' :
                        'Créer et assigner un itinéraire' }}</h2>
                </div>
                <button v-if="!embedded" type="button"
                    class="rounded-full p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                    aria-label="Fermer" @click="close">
                    <X :size="20" />
                </button>
            </div>

            <!-- BODY -->
            <div class="flex min-h-0 flex-1 flex-col gap-5 overflow-hidden">

                <!-- TOP ROW : ASSIGNMENT, ROUTE & MAP -->
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
                                <CreateSelect v-model="selectedDriverId" :options="driverOptions" :disabled="isSaving"
                                    label="Chauffeur" placeholder="Sélectionner un chauffeur" create-label="Nouveau chauffeur"
                                    @update:model-value="applyDriverVehicles"
                                    @create="showCreateDriver = true" />
                            </div>

                            <div class="space-y-1.5">
                                <label
                                    class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    <Truck :size="14" />
                                    Tracteur
                                </label>
                                <CreateSelect v-model="tractorId" :options="tractorOptions" :disabled="isSaving"
                                    label="Tracteur" placeholder="Sélectionner un tracteur" create-label="Nouveau véhicule"
                                    @create="showCreateTractor = true" />
                            </div>

                            <div class="space-y-1.5">
                                <label
                                    class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    <Truck :size="14" />
                                    Semi-remorque
                                </label>
                                <CreateSelect v-model="semiTrailerId" :options="semiTrailerOptions" :disabled="isSaving"
                                    label="Semi-remorque" placeholder="Sélectionner une semi-remorque"
                                    create-label="Nouveau véhicule" @create="showCreateSemiTrailer = true" />
                            </div>

                            <div class="space-y-1.5">
                                <label
                                    class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                    <Building2 :size="14" />
                                    Donneur d’ordre
                                </label>
                                <CreateSelect v-model="selectedCustomerId" :options="customerOptions" :disabled="isSaving"
                                    label="Donneur d’ordre" placeholder="Aucun donneur d’ordre"
                                    create-label="Nouveau donneur d’ordre" @create="showCreateCustomer = true" />
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
                                <legend class="text-sm font-semibold text-slate-700">Durées des services (minutes)
                                </legend>
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
                        </div>
                    </section>

                    <!-- COLUMN 2 : SEARCH -->
                    <section class="flex min-h-0 flex-col pr-1">
                        <div class="min-h-0 flex-1 rounded-2xl border border-slate-200 p-4 flex flex-col">
                            <div class="mb-4 grid shrink-0 grid-cols-2 gap-1 rounded-xl bg-slate-100 p-1" role="group"
                                aria-label="Mode de création">
                                <button v-for="mode in entryModes" :key="mode" type="button" :disabled="isSaving"
                                    :aria-pressed="activeMode === mode"
                                    class="flex items-center justify-center gap-2 rounded-lg px-2 py-2.5 text-sm font-semibold transition disabled:opacity-50"
                                    :class="activeMode === mode ? 'bg-white text-blue-700 shadow-sm' : 'text-slate-500 hover:text-slate-800'"
                                    @click="isDashdocSimulationEnabled = mode === 'dashdoc'">
                                    <component :is="mode === 'manual' ? RouteIcon : CalendarClock" :size="16" />
                                    {{ mode === 'manual' ? 'Manuel' : 'Simulation Dashdoc' }}
                                </button>
                            </div>
                            <div class="custom-scrollbar min-h-0 flex-1 overflow-auto pr-2">
                                <div v-for="mode in entryModes" :key="`${formSession}-${mode}`"
                                    v-show="activeMode === mode">
                                    <RouteForm :initial-request="initialFormRequest"
                                        :service-timing="mode === 'dashdoc' ? simulationTiming : undefined"
                                        v-model:tractor-id="tractorId" v-model:semi-trailer-id="semiTrailerId"
                                        v-model:driver-id="driverId" v-model:empty-trip="emptyTrip"
                                        :removable-events="!!editingTransport"
                                        @remove-event="eventToDelete = $event"
                                        @changed="modeStates[mode].dirty = true"
                                        @route-calculated="handleRouteCalculated($event, mode)">
                                        <fieldset v-if="mode === 'dashdoc'" class="space-y-3">
                                            <legend
                                                class="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
                                                Fenêtres horaires du service
                                            </legend>
                                            <label v-for="field in windowFields" :key="field.key"
                                                class="block text-xs font-medium text-slate-600">
                                                {{ field.label }}
                                                <input v-model="serviceWindows[field.key]" type="datetime-local"
                                                    step="60"
                                                    class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm transition focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20" />
                                            </label>
                                        </fieldset>
                                    </RouteForm>
                                </div>
                                <p v-if="activeState.dirty" class="mt-3 text-xs text-amber-700" role="status">
                                    Recalculez l’itinéraire pour prendre en compte vos modifications.
                                </p>
                            </div>
                        </div>

                        <div v-if="!activeState.dirty && routeResponse?.routes?.length" class="mt-4 space-y-2">
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
    </component>
    <ConfirmDeleteModal :show="eventToDelete !== null" :loading="deletingEvent" title="Supprimer l’événement"
        message="Supprimer cet événement et son point de passage ? Le trajet sera recalculé et les modifications non enregistrées de la replanification seront remplacées."
        @close="!deletingEvent && (eventToDelete = null)" @confirm="confirmDeleteEvent" />
    <TractorModal :show="showCreateTractor" @close="showCreateTractor = false" @created="onTractorCreated" />
    <SemiTrailerModal :show="showCreateSemiTrailer" @close="showCreateSemiTrailer = false"
        @created="onSemiTrailerCreated" />
    <DriverModal :show="showCreateDriver" @close="showCreateDriver = false" @created="onDriverCreated" />
    <CustomerModal :show="showCreateCustomer" @close="showCreateCustomer = false" @created="onCustomerCreated" />
</template>
