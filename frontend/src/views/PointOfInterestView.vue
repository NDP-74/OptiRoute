<script setup lang="ts">
import { onMounted, ref } from 'vue'

import HereMap from '@/components/maps/HereMap.vue'
import HereAutocompleteInput from '@/components/maps/HereAutocompleteInput.vue'
import { createPointOfInterest, getPointsOfInterest } from '@/api/pointOfInterestApi'

//Variables

const mapRef = ref()

const selectedPlace = ref<any>(null)
const label = ref('')
const saving = ref(false)
const message = ref<{ type: 'success' | 'error', text: string } | null>(null)

//Functions

async function loadPointsOfInterest() {
    try {
        mapRef.value?.setPointsOfInterest(await getPointsOfInterest())
    } catch (e) {
        console.error(e)
    }
}

onMounted(loadPointsOfInterest)

function handlePlaceSelected(place: any) {
    selectedPlace.value = place
    label.value = ''
    message.value = null

    mapRef.value?.setPointMarker(place.position)
}

function handlePlaceChanged(place: any) {
    if (!place) {
        selectedPlace.value = null
        mapRef.value?.setPointMarker(null)
        return
    }

    // Saisie directe de coordonnées
    if (place !== selectedPlace.value && place.position) {
        handlePlaceSelected(place)
    }
}

async function save() {
    if (!selectedPlace.value || !label.value.trim()) return

    saving.value = true
    message.value = null

    try {
        await createPointOfInterest({
            label: label.value.trim(),
            address: selectedPlace.value.address ?? null,
            latitude: selectedPlace.value.position.lat,
            longitude: selectedPlace.value.position.lng
        })

        message.value = { type: 'success', text: "Point d'intérêt enregistré." }
        selectedPlace.value = null
        label.value = ''
        mapRef.value?.setPointMarker(null)
        await loadPointsOfInterest()
    } catch (e) {
        console.error(e)
        message.value = { type: 'error', text: "Impossible d'enregistrer le point d'intérêt." }
    } finally {
        saving.value = false
    }
}
</script>

<template>
    <div class="relative h-full w-full overflow-hidden">

        <!-- MAP -->
        <div class="absolute inset-0">
            <HereMap ref="mapRef" />
        </div>

        <!-- SEARCH PANEL -->
        <div class="absolute top-4 left-4 z-10 w-96 space-y-3 rounded-2xl bg-white p-4 shadow-xl">
            <h2 class="text-sm font-semibold text-slate-800">Ajouter un point d'intérêt</h2>

            <HereAutocompleteInput label="Rechercher une adresse" :model-value="selectedPlace"
                @update:model-value="handlePlaceChanged" />

            <template v-if="selectedPlace">
                <input v-model="label" type="text" maxlength="255" placeholder="Nom du point d'intérêt"
                    class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20" />

                <button type="button" :disabled="saving || !label.trim()" @click="save"
                    class="w-full rounded-xl bg-blue-600 px-3 py-2.5 text-sm font-medium text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                    {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
                </button>
            </template>

            <p v-if="message" class="text-xs" :class="message.type === 'success' ? 'text-green-600' : 'text-red-600'">
                {{ message.text }}
            </p>
        </div>

    </div>
</template>
