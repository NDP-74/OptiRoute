<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { isAxiosError } from 'axios'

import HereMap from '@/components/maps/HereMap.vue'
import HereAutocompleteInput from '@/components/maps/HereAutocompleteInput.vue'
import ConfirmDeleteModal from '@/components/ui/ConfirmDeleteModal.vue'
import { createPointOfInterest, deletePointOfInterest, getPointsOfInterest, updatePointOfInterest } from '@/api/pointOfInterestApi'
import { getApiErrorMessage } from '@/api/utils'
import type { PointOfInterest } from '@/models/PointOfInterest'

//Variables

const mapRef = ref<InstanceType<typeof HereMap> | null>(null)

interface SelectedPlace {
    address: string | null
    position: { lat: number, lng: number }
}

const points = ref<PointOfInterest[]>([])
const editingPoint = ref<PointOfInterest | null>(null)
const selectedPlace = ref<SelectedPlace | null>(null)
const addressResolved = ref(false)
const label = ref('')
const saving = ref(false)
const deleting = ref(false)
const showDeleteModal = ref(false)
const busy = computed(() => saving.value || deleting.value)
const message = ref<{ type: 'success' | 'error', text: string } | null>(null)

//Functions

async function loadPointsOfInterest() {
    try {
        points.value = await getPointsOfInterest()
        mapRef.value?.setPointsOfInterest(points.value)
    } catch (e) {
        console.error(e)
        message.value = { type: 'error', text: getApiErrorMessage(e, "Impossible de charger les points d'intérêt.") }
    }
}

onMounted(loadPointsOfInterest)

function handlePlaceSelected(place: SelectedPlace) {
    selectedPlace.value = place
    addressResolved.value = true
    if (!editingPoint.value) label.value = ''
    message.value = null

    mapRef.value?.setPointMarker(place.position)
}

function handlePlaceChanged(place: SelectedPlace | null) {
    if (busy.value) return
    if (!place) {
        selectedPlace.value = null
        addressResolved.value = false
        mapRef.value?.setPointMarker(null)
        return
    }

    // Saisie directe de coordonnées
    if (place !== selectedPlace.value && place.position) {
        handlePlaceSelected(place)
    }
}

function selectPoint(point: PointOfInterest) {
    if (busy.value || showDeleteModal.value) return

    editingPoint.value = point
    selectedPlace.value = {
        address: point.address,
        position: { lat: Number(point.latitude), lng: Number(point.longitude) }
    }
    label.value = point.label
    addressResolved.value = true
    message.value = null
    mapRef.value?.setPointMarker(null)
}

function resetForm() {
    editingPoint.value = null
    selectedPlace.value = null
    addressResolved.value = false
    label.value = ''
    showDeleteModal.value = false
    message.value = null
    mapRef.value?.setPointMarker(null)
}

async function save() {
    if (busy.value || !selectedPlace.value || !addressResolved.value || !label.value.trim()) return

    saving.value = true
    message.value = null

    try {
        const payload = {
            label: label.value.trim(),
            address: selectedPlace.value.address ?? null,
            latitude: selectedPlace.value.position.lat,
            longitude: selectedPlace.value.position.lng
        }
        const point = editingPoint.value
            ? await updatePointOfInterest(editingPoint.value.id, payload)
            : await createPointOfInterest(payload)

        points.value = [...points.value.filter(item => item.id !== point.id), point]
        mapRef.value?.setPointsOfInterest(points.value)
        resetForm()
        message.value = { type: 'success', text: "Point d'intérêt enregistré." }
    } catch (e) {
        console.error(e)
        message.value = { type: 'error', text: getApiErrorMessage(e, "Impossible d'enregistrer le point d'intérêt.") }
    } finally {
        saving.value = false
    }
}

async function removePoint() {
    if (busy.value || !editingPoint.value) return

    deleting.value = true
    message.value = null

    try {
        await deletePointOfInterest(editingPoint.value.id)
        points.value = points.value.filter(point => point.id !== editingPoint.value?.id)
        mapRef.value?.setPointsOfInterest(points.value)
        resetForm()
        message.value = { type: 'success', text: "Point d'intérêt supprimé." }
    } catch (e) {
        console.error(e)
        showDeleteModal.value = false
        message.value = {
            type: 'error',
            text: isAxiosError(e) && e.response?.status === 409
                ? "Ce point d'intérêt est utilisé par un événement et ne peut pas être supprimé."
                : getApiErrorMessage(e, "Impossible de supprimer le point d'intérêt.")
        }
    } finally {
        deleting.value = false
    }
}
</script>

<template>
    <div class="relative h-full w-full overflow-hidden">

        <!-- MAP -->
        <div class="absolute inset-0">
            <HereMap ref="mapRef" @point-of-interest-selected="selectPoint" />
        </div>

        <!-- SEARCH PANEL -->
        <div class="absolute top-4 left-4 z-10 w-96 space-y-3 rounded-2xl bg-white p-4 shadow-xl">
            <h2 class="text-sm font-semibold text-slate-800">
                {{ editingPoint ? "Modifier le point d'intérêt" : "Ajouter un point d'intérêt" }}
            </h2>

            <HereAutocompleteInput label="Rechercher une adresse" :model-value="selectedPlace" :disabled="busy"
                @input="addressResolved = false" @update:model-value="handlePlaceChanged" />

            <p v-if="selectedPlace && !addressResolved" class="text-xs text-slate-500">
                Sélectionnez une adresse proposée ou saisissez des coordonnées valides (latitude, longitude).
            </p>

            <template v-if="selectedPlace">
                <input v-model="label" type="text" maxlength="255" :disabled="busy" aria-label="Nom du point d'intérêt" placeholder="Nom du point d'intérêt"
                    class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20" />

                <button type="button" :disabled="busy || !addressResolved || !label.trim()" @click="save"
                    class="w-full rounded-xl bg-blue-600 px-3 py-2.5 text-sm font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                    {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
                </button>
            </template>

            <div v-if="editingPoint" class="flex gap-2">
                <button type="button" :disabled="busy" @click="resetForm"
                    class="flex-1 rounded-xl border border-slate-300 px-3 py-2.5 text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-50">
                    Annuler
                </button>
                <button type="button" :disabled="busy" @click="showDeleteModal = true"
                    class="flex-1 rounded-xl bg-red-600 px-3 py-2.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50">
                    Supprimer
                </button>
            </div>

            <p v-if="message" role="status" class="text-xs" :class="message.type === 'success' ? 'text-green-600' : 'text-red-600'">
                {{ message.text }}
            </p>
        </div>

        <ConfirmDeleteModal :show="showDeleteModal" :loading="deleting"
            title="Supprimer le point d'intérêt"
            :message="`Voulez-vous vraiment supprimer le point d'intérêt « ${editingPoint?.label ?? ''} » ?`"
            @close="!deleting && (showDeleteModal = false)" @confirm="removePoint" />

    </div>
</template>
