<script setup lang="ts">
import { computed, ref, watch } from "vue"

import { getTractors } from "@/api/vehicle/tractorApi"
import { getSemiTrailers } from "@/api/vehicle/semiTrailerApi"
import { createVehicleEvent, updateVehicleEvent } from "@/api/vehicleEventApi"
import { getApiErrorMessage } from "@/api/utils"
import AppModal from "@/components/ui/AppModal.vue"
import { useNotification } from "@/composables/useNotification"

import type { TractorSummary } from "@/models/vehicle/Tractor"
import type { SemiTrailerSummary } from "@/models/vehicle/SemiTrailer"
import type { VehicleEventRequest, VehicleEventResponse } from "@/models/vehicle/VehicleEvent"

const props = defineProps<{
    show: boolean
    event?: VehicleEventResponse | null
}>()

const emit = defineEmits<{
    close: []
    created: []
    updated: []
}>()

const notification = useNotification()
const tractors = ref<TractorSummary[]>([])
const semiTrailers = ref<SemiTrailerSummary[]>([])
const loadingOptions = ref(false)
const loading = ref(false)

const formatDate = (date: Date): string => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, "0")
    const day = String(date.getDate()).padStart(2, "0")

    return `${year}-${month}-${day}`
}

const createEmptyForm = (): VehicleEventRequest => ({
    eventDate: formatDate(new Date()),
    supplier: null,
    cost: null,
    tractorId: null,
    semiTrailerId: null,
})

const form = ref<VehicleEventRequest>(createEmptyForm())
const selectedVehicleType = ref<"tractor" | "semiTrailer">("tractor")
const isValid = computed(() => form.value.eventDate && form.value.cost !== null && form.value.cost >= 0
    && (form.value.tractorId !== null || form.value.semiTrailerId !== null))

const resetForm = () => {
    form.value = createEmptyForm()
    selectedVehicleType.value = "tractor"
}

const populateForm = (event: VehicleEventResponse) => {
    form.value = {
        eventDate: event.eventDate,
        supplier: event.supplier,
        cost: event.cost,
        tractorId: event.tractorId,
        semiTrailerId: event.semiTrailerId,
    }
    selectedVehicleType.value = event.tractorId !== null ? "tractor" : "semiTrailer"
}

const loadVehicleOptions = async () => {
    try {
        loadingOptions.value = true
        const [tractorOptions, semiTrailerOptions] = await Promise.all([getTractors(), getSemiTrailers()])
        tractors.value = tractorOptions
        semiTrailers.value = semiTrailerOptions
    } catch (error) {
        notification.error("Chargement impossible", getApiErrorMessage(error, "Les véhicules n'ont pas pu être chargés."))
    } finally {
        loadingOptions.value = false
    }
}

const selectVehicleType = (type: "tractor" | "semiTrailer") => {
    selectedVehicleType.value = type

    if (type === "tractor") {
        form.value.semiTrailerId = null
        return
    }

    form.value.tractorId = null
}

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

const submitEvent = async () => {
    if (!isValid.value) {
        return
    }

    try {
        loading.value = true
        const payload = {
            ...form.value,
            supplier: form.value.supplier?.trim() || null,
        }

        if (props.event) {
            await updateVehicleEvent(props.event.id, payload)
            notification.success("Événement modifié", "L'événement véhicule a bien été modifié.")
            emit("updated")
            return
        }

        await createVehicleEvent(payload)
        notification.success("Événement créé", "L'événement véhicule a bien été ajouté au planning.")
        emit("created")
    } catch (error) {
        notification.error("Création impossible", getApiErrorMessage(error, "L'événement véhicule n'a pas pu être créé."))
    } finally {
        loading.value = false
    }
}

watch(
    () => props.show,
    (show) => {
        if (show) {
            if (props.event) {
                populateForm(props.event)
            } else {
                resetForm()
            }
            void loadVehicleOptions()
        }
    },
)
</script>

<template>
    <AppModal :show="show" panel-class="max-w-2xl" @close="closeModal">
        <form @submit.prevent="submitEvent">
            <h2 class="text-2xl font-bold text-slate-900">
                {{ event ? "Modifier l'événement véhicule" : "Créer un événement véhicule" }}
            </h2>
            <p class="mt-1 text-sm text-slate-500">
                Enregistrez un coût ponctuel lié à un véhicule.
            </p>

            <div class="mt-6 max-h-[65vh] space-y-6 overflow-y-auto pr-2">
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                        <label class="mb-1.5 block text-sm font-medium text-slate-700">Date</label>
                        <input v-model="form.eventDate" type="date" required :disabled="loading"
                            class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                    </div>
                    <div>
                        <label class="mb-1.5 block text-sm font-medium text-slate-700">Coût</label>
                        <div class="relative">
                            <input v-model.number="form.cost" type="number" min="0" step="0.01" required
                                :disabled="loading"
                                class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 pr-10 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                            <span
                                class="pointer-events-none absolute inset-y-0 right-4 flex items-center text-sm text-slate-400">€</span>
                        </div>
                    </div>
                </div>

                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">Fournisseur</label>
                    <input v-model="form.supplier" type="text" maxlength="255" :disabled="loading"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>

                <section class="space-y-4 border-t border-slate-200 pt-6">
                    <div>
                        <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">Véhicule concerné</h3>
                        <p class="mt-1 text-sm text-slate-500">Sélectionnez un tracteur ou une semi-remorque.</p>
                    </div>

                    <div class="grid grid-cols-2 gap-3">
                        <button type="button" :disabled="loading"
                            class="rounded-xl border p-3 text-left text-sm font-medium transition"
                            :class="selectedVehicleType === 'tractor' ? 'border-blue-500 bg-blue-50 text-blue-800 ring-1 ring-blue-500' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
                            @click="selectVehicleType('tractor')">
                            Tracteur
                        </button>
                        <button type="button" :disabled="loading"
                            class="rounded-xl border p-3 text-left text-sm font-medium transition"
                            :class="selectedVehicleType === 'semiTrailer' ? 'border-blue-500 bg-blue-50 text-blue-800 ring-1 ring-blue-500' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
                            @click="selectVehicleType('semiTrailer')">
                            Semi-remorque
                        </button>
                    </div>

                    <div v-if="selectedVehicleType === 'tractor'">
                        <label class="mb-1.5 block text-sm font-medium text-slate-700">Tracteur</label>
                        <select v-model="form.tractorId" :disabled="loading || loadingOptions"
                            class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100">
                            <option :value="null">Sélectionner un tracteur</option>
                            <option v-for="tractor in tractors" :key="tractor.id" :value="tractor.id">{{
                                tractor.registration }}</option>
                        </select>
                    </div>

                    <div v-else>
                        <label class="mb-1.5 block text-sm font-medium text-slate-700">Semi-remorque</label>
                        <select v-model="form.semiTrailerId" :disabled="loading || loadingOptions"
                            class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100">
                            <option :value="null">Sélectionner une semi-remorque</option>
                            <option v-for="semiTrailer in semiTrailers" :key="semiTrailer.id" :value="semiTrailer.id">{{
                                semiTrailer.registration }}</option>
                        </select>
                    </div>
                </section>
            </div>

            <div class="mt-6 flex justify-end gap-3">
                <button type="button" :disabled="loading"
                    class="rounded-xl border border-slate-300 px-4 py-2 text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                    @click="closeModal">Annuler</button>
                <button type="submit" :disabled="loading || !isValid"
                    class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                    {{ loading ? "Enregistrement" : event ? "Modifier" : "Enregistrer" }}
                </button>
            </div>
        </form>
    </AppModal>
</template>