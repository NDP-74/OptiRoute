<script setup lang="ts">
import { computed, ref, watch } from "vue"
import { CalendarDays, Edit3, Truck, Container, Trash2, Wrench } from "lucide-vue-next"

import { deleteVehicleEvent, getVehicleEvent } from "@/api/vehicleEventApi"
import { getApiErrorMessage } from "@/api/utils"
import CreateVehicleEventModal from "@/components/planning/CreateVehicleEventModal.vue"
import ConfirmDeleteModal from "@/components/ui/ConfirmDeleteModal.vue"
import AppDetailDrawer from "@/components/ui/AppDetailDrawer.vue"
import DetailRow from "@/components/ui/DetailRow.vue"
import DetailSection from "@/components/ui/DetailSection.vue"
import { useNotification } from "@/composables/useNotification"
import { formatCurrency, formatDate } from "@/utils/formatters"

import type { VehicleEventResponse } from "@/models/vehicle/VehicleEvent"

const props = defineProps<{
    open: boolean
    eventId: number | null
}>()

const emit = defineEmits<{
    close: []
    updated: []
    deleted: []
}>()

const notification = useNotification()
const event = ref<VehicleEventResponse | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const showUpdateModal = ref(false)
const showDeleteModal = ref(false)
const deleting = ref(false)
let currentRequestId = 0

const vehicleRegistration = computed(() => event.value?.tractorRegistration ?? event.value?.semiTrailerRegistration ?? "Non renseigné")
const vehicleType = computed(() => event.value?.tractorId !== null ? "Tracteur" : "Semi-remorque")
const vehicleIcon = computed(() => event.value?.tractorId !== null ? Truck : Container)

const loadEvent = async () => {
    if (props.eventId === null) {
        event.value = null
        return
    }

    const requestId = ++currentRequestId

    try {
        loading.value = true
        error.value = null
        event.value = await getVehicleEvent(props.eventId)
    } catch {
        if (requestId === currentRequestId) {
            error.value = "Impossible de récupérer les informations de l'événement."
        }
    } finally {
        if (requestId === currentRequestId) {
            loading.value = false
        }
    }
}

const handleUpdated = async () => {
    showUpdateModal.value = false
    await loadEvent()
    emit("updated")
}

const confirmDelete = async () => {
    if (!event.value) {
        return
    }

    try {
        deleting.value = true
        await deleteVehicleEvent(event.value.id)
        notification.success("Événement supprimé", "L'événement véhicule a bien été supprimé.")
        showDeleteModal.value = false
        event.value = null
        emit("deleted")
        emit("close")
    } catch (exception) {
        notification.error("Suppression impossible", getApiErrorMessage(exception, "L'événement véhicule n'a pas pu être supprimé."))
    } finally {
        deleting.value = false
    }
}

watch(
    () => [props.open, props.eventId],
    ([open, eventId]) => {
        if (open && eventId !== null) {
            void loadEvent()
            return
        }

        currentRequestId++
        event.value = null
        error.value = null
        loading.value = false
        showUpdateModal.value = false
        showDeleteModal.value = false
    },
    { immediate: true },
)
</script>

<template>
    <AppDetailDrawer :open="open" title="Détail de l'événement" @close="emit('close')">
        <template #header>
            <div class="min-w-0">
                <p class="text-xs font-medium uppercase tracking-wide text-blue-600">Détails de l'événement</p>
                <h2 class="mt-1 truncate text-lg font-semibold text-slate-900">{{ event?.supplier || "Événement " +
                    "véhicule" }}</h2>
                <p v-if="event" class="mt-0.5 truncate text-sm text-slate-500">{{ vehicleRegistration }}</p>
            </div>
        </template>

        <div v-if="loading" class="flex min-h-full items-center justify-center">
            <div class="flex items-center gap-3 text-sm text-slate-500">
                <div class="h-5 w-5 animate-spin rounded-full border-2 border-slate-300 border-t-slate-700" />Chargement
                de
                l'événement...
            </div>
        </div>

        <div v-else-if="error" class="flex min-h-full flex-col items-center justify-center gap-4 px-6 text-center">
            <div>
                <p class="font-medium text-slate-800">Impossible de charger l'événement</p>
                <p class="mt-1 text-sm text-slate-500">{{ error }}</p>
            </div>
            <button type="button"
                class="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                @click="loadEvent">Réessayer</button>
        </div>

        <div v-else-if="event" class="space-y-4 p-4">
            <section class="rounded-xl border border-blue-200 bg-blue-50 p-4 shadow-sm">
                <div class="flex items-center gap-2 text-blue-900">
                    <Wrench class="h-5 w-5 text-blue-600" />
                    <h3 class="font-semibold">Événement véhicule</h3>
                </div>
                <p class="mt-3 text-2xl font-bold text-blue-900">{{ formatCurrency(event.cost) }}</p>
            </section>

            <DetailSection title="Informations" :icon="CalendarDays">
                <DetailRow label="Date" :value="formatDate(event.eventDate)" />
                <DetailRow label="Fournisseur" :value="event.supplier || 'Non renseigné'" />
                <DetailRow label="Coût" :value="formatCurrency(event.cost)" />
            </DetailSection>

            <DetailSection title="Véhicule concerné" :icon="vehicleIcon">
                <DetailRow label="Type" :value="vehicleType" />
                <DetailRow label="Immatriculation" :value="vehicleRegistration" />
            </DetailSection>
        </div>

        <template v-if="event" #footer>
            <div class="flex items-center justify-between gap-3">
                <button type="button"
                    class="inline-flex items-center gap-2 rounded-xl border border-red-200 px-4 py-2 text-sm font-medium text-red-600 transition hover:bg-red-50"
                    @click="showDeleteModal = true">
                    <Trash2 class="h-4 w-4" />Supprimer
                </button>
                <button type="button"
                    class="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700"
                    @click="showUpdateModal = true">
                    <Edit3 class="h-4 w-4" />Modifier
                </button>
            </div>
        </template>
    </AppDetailDrawer>

    <CreateVehicleEventModal :show="showUpdateModal" :event="event" @close="showUpdateModal = false"
        @updated="handleUpdated" />
    <ConfirmDeleteModal :show="showDeleteModal" :loading="deleting" title="Supprimer l'événement"
        message="Voulez-vous vraiment supprimer cet événement véhicule ?" @close="showDeleteModal = false"
        @confirm="confirmDelete" />
</template>