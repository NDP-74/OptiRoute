<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Settings2, X } from 'lucide-vue-next'

import AppModal from '@/components/ui/AppModal.vue'
import PlanningRouteModal from '@/components/planning/PlanningRouteModal.vue'
import RouteSegmentPanel from '@/components/planning/RouteSegmentPanel.vue'
import ServiceInfoPanel from '@/components/planning/ServiceInfoPanel.vue'

import { getEventsByService } from '@/api/eventApi'
import { getTransportById } from '@/api/planningApi'
import { getApiErrorMessage } from '@/api/utils'
import { useNotification } from '@/composables/useNotification'

import type { EventResponse } from '@/models/Event'
import type { TransportDetail } from '@/models/transport/TransportDetail'

const SERVICE_TAB = 'service'

const props = defineProps<{
    show: boolean
    transport: TransportDetail | null
    // Trajet affiché dans le tiroir, ouvert par défaut
    initialRouteId: number | null
}>()

const emit = defineEmits<{
    close: []
    saved: []
}>()

const notification = useNotification()
const activeTab = ref<number | typeof SERVICE_TAB>(SERVICE_TAB)
const replanning = ref(false)
const events = ref<EventResponse[]>([])
const loadingEvents = ref(false)
const editingTransport = ref<TransportDetail | null>(null)
const revisions = ref<Record<string, number>>({})
const refreshing = ref(false)

const routes = computed(() => editingTransport.value?.routes ?? [])
const activeRoute = computed(() => routes.value.find(route => route.routeId === activeTab.value) ?? null)

watch(() => props.show, async show => {
    if (!show || !props.transport) return

    editingTransport.value = props.transport
    revisions.value = {}
    replanning.value = false
    activeTab.value = routes.value.some(route => route.routeId === props.initialRouteId)
        ? props.initialRouteId!
        : routes.value[0]?.routeId ?? SERVICE_TAB
    events.value = []
    loadingEvents.value = true
    try {
        const transport = props.transport
        const eventsByRoute = await Promise.all(routes.value.map(route => getEventsByService(transport.id, route.routeId)))
        events.value = eventsByRoute.flat()
    } catch (error) {
        notification.error('Chargement impossible', getApiErrorMessage(error, 'Les événements du service n’ont pas pu être chargés.'))
        emit('close')
    } finally {
        loadingEvents.value = false
    }
})

async function handleSaved() {
    if (!editingTransport.value || refreshing.value) return
    const tab = activeTab.value
    const wasReplanning = replanning.value
    refreshing.value = true
    emit('saved')
    try {
        const updated = await getTransportById(editingTransport.value.id)
        const eventsByRoute = await Promise.all(updated.routes.map(route => getEventsByService(updated.id, route.routeId)))
        editingTransport.value = updated
        events.value = eventsByRoute.flat()
        if (wasReplanning) {
            for (const key of [SERVICE_TAB, ...updated.routes.map(route => route.routeId)]) {
                revisions.value[key] = (revisions.value[key] ?? 0) + 1
            }
            activeTab.value = SERVICE_TAB
            replanning.value = false
        } else {
            revisions.value[tab] = (revisions.value[tab] ?? 0) + 1
        }
    } catch (error) {
        notification.error('Actualisation impossible', getApiErrorMessage(error, 'La modification est enregistrée, mais le formulaire n’a pas pu être actualisé.'))
    } finally {
        refreshing.value = false
    }
}

function eventsForRoute(routeId: number): EventResponse[] {
    return events.value.filter(event => event.routeId === routeId)
}

function close() {
    emit('close')
}
</script>

<template>
    <AppModal :show="show" panel-class="max-w-screen-2xl" @close="close">
        <PlanningRouteModal v-if="editingTransport && replanning" embedded :show="true" :editing-transport="editingTransport"
            :events="events" @close="replanning = false" @saved="handleSaved" />

        <div v-if="editingTransport" v-show="!replanning" class="flex h-[82vh] max-h-[720px] flex-col">
            <div class="flex shrink-0 items-start justify-between gap-4">
                <div class="min-w-0">
                    <h2 class="truncate text-xl font-bold text-slate-900">Modifier « {{ editingTransport.name }} »</h2>
                    <p class="text-sm text-slate-500">Chaque onglet s’enregistre séparément.</p>
                </div>
                <button type="button"
                    class="rounded-full p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                    aria-label="Fermer" @click="close">
                    <X :size="20" />
                </button>
            </div>

            <div class="mt-4 flex shrink-0 gap-1 overflow-x-auto overflow-y-hidden border-b border-slate-200" role="tablist">
                <button type="button" role="tab" :aria-selected="activeTab === SERVICE_TAB"
                    class="mr-2 inline-flex shrink-0 items-center gap-2 rounded-t-xl border-b-2 px-4 py-2 text-sm font-bold transition"
                    :class="activeTab === SERVICE_TAB ? 'border-blue-600 bg-blue-100 text-blue-800' : 'border-transparent bg-blue-50 text-blue-700 hover:bg-blue-100'"
                    @click="activeTab = SERVICE_TAB">
                    <Settings2 class="h-4 w-4" />
                    Service
                </button>
                <button v-for="(route, index) in routes" :key="route.routeId" type="button" role="tab"
                    :aria-selected="activeTab === route.routeId"
                    class="shrink-0 border-b-2 px-4 py-2 text-left transition"
                    :class="activeTab === route.routeId ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:text-slate-800'"
                    @click="activeTab = route.routeId">
                    <span class="block text-sm font-semibold">Trajet {{ index + 1 }}</span>
                    <span class="block max-w-56 truncate text-xs">{{ route.originName }} → {{ route.destinationName
                        }}</span>
                </button>
            </div>

            <div class="min-h-0 flex-1 pt-4">
                <div v-if="loadingEvents" class="flex h-full items-center justify-center gap-3 text-sm text-slate-500">
                    <div class="h-5 w-5 animate-spin rounded-full border-2 border-slate-300 border-t-slate-700" />
                    Chargement…
                </div>
                <!-- Les onglets visités restent montés : les modifications en cours ne sont pas perdues en changeant d'onglet -->
                <KeepAlive v-else>
                    <RouteSegmentPanel v-if="activeRoute" :key="`${activeRoute.routeId}-${revisions[activeRoute.routeId] ?? 0}`" :transport="editingTransport"
                        :route-id="activeRoute.routeId" :events="eventsForRoute(activeRoute.routeId)" @cancel="close"
                        @saved="handleSaved" />
                    <ServiceInfoPanel v-else :key="`${SERVICE_TAB}-${revisions[SERVICE_TAB] ?? 0}`" :transport="editingTransport" @cancel="close"
                        @saved="handleSaved" @replan="replanning = true" />
                </KeepAlive>
            </div>
        </div>
    </AppModal>
</template>
