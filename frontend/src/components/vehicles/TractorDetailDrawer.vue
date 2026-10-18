<script setup lang="ts">
import { ref, watch } from "vue"

import {
    CircleDollarSign,
    Info,
    Trash2,
    Edit3,
} from "lucide-vue-next"

import type { TractorDetails } from "@/models/vehicle/Tractor"

import type { EventResponse } from "@/models/Event"

import { getTractor } from "@/api/vehicle/tractorApi"
import { getEventsByTractor } from "@/api/eventApi"
import { getApiErrorMessage } from "@/api/utils"


import AppDetailDrawer from "@/components/ui/AppDetailDrawer.vue"
import DetailSection from "@/components/ui/DetailSection.vue"
import DetailRow from "@/components/ui/DetailRow.vue"
import UpdateTractorModal from "@/components/vehicles/UpdateTractorModal.vue"
import DeleteTractorModal from "@/components/vehicles/DeleteTractorModal.vue"
import VehicleEventsSection from "@/components/vehicles/VehicleEventsSection.vue"

import {
    formatCurrency, formatDate,
} from "@/utils/formatters"

//Variables

const props = defineProps<{
    open: boolean
    tractorId: number | null
}>()

const emit = defineEmits<{
    close: []
    deleted: []
    updated: []
}>()

const tractor = ref<TractorDetails | null>(null)

const loading = ref(false)
const error = ref<string | null>(null)

const events = ref<EventResponse[]>([])
const eventsLoading = ref(false)
const eventsError = ref<string | null>(null)

/*
 * Permet d’ignorer une ancienne réponse HTTP lorsque
 * l’utilisateur sélectionne rapidement plusieurs tracteurs.
 */
let currentRequestId = 0

const loadEvents = async (tractorId: number, requestId: number) => {
    events.value = []
    eventsLoading.value = true
    eventsError.value = null

    try {
        const loadedEvents = await getEventsByTractor(tractorId)

        if (requestId === currentRequestId) {
            events.value = loadedEvents
        }
    } catch (err) {
        if (requestId === currentRequestId) {
            eventsError.value = getApiErrorMessage(err, "Impossible de charger les événements du tracteur.")
        }
    } finally {
        if (requestId === currentRequestId) {
            eventsLoading.value = false
        }
    }
}

const loadTractor = async () => {
    if (props.tractorId === null) {
        return
    }

    const requestId = ++currentRequestId

    const tractorId = props.tractorId

    loading.value = true
    error.value = null
    tractor.value = null

    loadEvents(tractorId, requestId)

    try {
        const loadedTractor = await getTractor(tractorId)

        if (requestId === currentRequestId) {
            tractor.value = loadedTractor
        }
    } catch (err) {
        if (requestId !== currentRequestId) {
            return
        }

        error.value = getApiErrorMessage(err, "Impossible de charger les informations de la semi-remorque.")
    } finally {
        if (requestId === currentRequestId) {
            loading.value = false
        }
    }
}

//Manage Modal

const showDeleteModal = ref(false)
const showUpdateModal = ref(false)

const openUpdateModal = () => {
    if (!tractor.value) {
        return
    }

    showUpdateModal.value = true
}

const closeUpdateModal = () => {
    showUpdateModal.value = false
}

const openDeleteModal = () => {
    if (!tractor.value) {
        return
    }

    showDeleteModal.value = true
}

const closeDeleteModal = () => {
    showDeleteModal.value = false
}

//Handle

const handleTractorUpdated = async () => {
    showUpdateModal.value = false

    await loadTractor()
    emit("updated")
}

const handleTractorDeleted = () => {
    showDeleteModal.value = false
    tractor.value = null

    emit("deleted")
    emit("close")
}

//Watch

watch(
    [() => props.open, () => props.tractorId],
    ([open, tractorId]) => {
        if (open && tractorId !== null) {
            loadTractor()
            return
        }

        /* Invalide la requête en cours lors de la fermeture. */
        currentRequestId++

        tractor.value = null
        error.value = null
        loading.value = false
        events.value = []
        eventsError.value = null
        eventsLoading.value = false

        showUpdateModal.value = false
        showDeleteModal.value = false
    },
    {
        immediate: true,
    },
)

</script>

<template>
    <AppDetailDrawer :open="props.open" :title="tractor?.registration ?? 'Détail du tracteur'" @close="emit('close')">
        <template #header>
            <div class="min-w-0">
                <p class="text-xs font-medium uppercase tracking-wide text-blue-600">
                    Détails du tracteur
                </p>

                <h2 class="mt-1 truncate text-xl font-bold text-slate-900">
                    {{ tractor?.registration ?? "Chargement..." }}
                </h2>
            </div>
        </template>

        <!-- Chargement -->
        <div v-if="loading" class="flex min-h-full items-center justify-center">
            <div class="text-center">
                <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-slate-200 border-t-blue-600" />

                <p class="mt-3 text-sm text-slate-500">
                    Chargement des informations...
                </p>
            </div>
        </div>

        <!-- Erreur -->
        <div v-else-if="error" class="flex min-h-full items-center justify-center p-6">
            <div class="w-full rounded-2xl border border-red-200 bg-red-50 p-5 text-center">
                <p class="font-medium text-red-800">
                    Chargement impossible
                </p>

                <p class="mt-2 text-sm text-red-700">
                    {{ error }}
                </p>

                <button type="button"
                    class="mt-4 rounded-xl bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700"
                    @click="loadTractor">
                    Réessayer
                </button>
            </div>
        </div>

        <!-- Contenu -->
        <div v-else-if="tractor" class="space-y-6 p-6">
            <!-- Identification -->
            <DetailSection title="Identification" :icon="Info">
                <DetailRow label="Immatriculation" :value="tractor.registration" />
            </DetailSection>

            <!-- Achat et amortissement -->
            <DetailSection title="Achat et amortissement" :icon="CircleDollarSign">
                <DetailRow label="Coût d’achat" :value="formatCurrency(tractor.purchaseCost)" />

                <DetailRow label="Début d’amortissement" :value="formatDate(tractor.depreciationStartDate)" />

                <DetailRow label="Fin d’amortissement" :value="formatDate(tractor.depreciationEndDate)" />
            </DetailSection>

            <!-- Coûts -->
            <DetailSection title="Coûts" :icon="CircleDollarSign">
                <DetailRow label="Entretien" :value="formatCurrency(tractor.maintenanceCostPerKm) + ' / km'" />
            </DetailSection>

            <VehicleEventsSection :events="events" :loading="eventsLoading" :error="eventsError" />
        </div>

        <!-- Actions -->
        <template v-if="tractor" #footer>
            <div class="flex items-center justify-between gap-3">
                <button type="button"
                    class="inline-flex items-center gap-2 rounded-xl border border-red-200 px-4 py-2 text-sm font-medium text-red-600 transition hover:bg-red-50"
                    @click="openDeleteModal">
                    <Trash2 class="h-4 w-4" />
                    Supprimer
                </button>

                <button type="button"
                    class="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700"
                    @click="openUpdateModal">
                    <Edit3 class="h-4 w-4" />
                    Modifier
                </button>
            </div>
        </template>
    </AppDetailDrawer>

    <UpdateTractorModal :show="showUpdateModal" :tractor="tractor" @close="closeUpdateModal"
        @updated="handleTractorUpdated" />

    <DeleteTractorModal :show="showDeleteModal" :tractor="tractor" @close="closeDeleteModal"
        @deleted="handleTractorDeleted" />
</template>