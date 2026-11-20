<script setup lang="ts">
import { reactive, ref, watch } from "vue"

import { createSemiTrailer, getSemiTrailerTypes, updateSemiTrailer } from "@/api/vehicle/semiTrailerApi"
import type { TrailerTypeOption } from "@/api/vehicle/semiTrailerApi"
import { getApiErrorMessage } from "@/api/utils"

import type { CreateSemiTrailerRequest, SemiTrailerDetails, SemiTrailerFormData, SemiTrailerSummary } from "@/models/vehicle/SemiTrailer"
import {
    createEmptySemiTrailerForm,
    formatVehicleRegistration,
    normalizeVehicleRegistration,
} from "@/utils/vehicleUtils"

import AppModal from "@/components/ui/AppModal.vue"
import SemiTrailerForm from "@/components/vehicles/SemiTrailerForm.vue"

import { useNotification } from "@/composables/useNotification"

const notification = useNotification()
const loading = ref(false)
const trailerTypes = ref<TrailerTypeOption[]>([])

const props = defineProps<{
    show: boolean
    semiTrailer?: SemiTrailerDetails | null
}>()

const emit = defineEmits<{
    close: []
    created: [semiTrailer: SemiTrailerSummary]
    updated: []
}>()

const form = reactive<SemiTrailerFormData>(createEmptySemiTrailerForm())

const syncForm = () => {
    if (props.semiTrailer) {
        Object.assign(form, {
            registration: normalizeVehicleRegistration(props.semiTrailer.registration),
            trailerType: props.semiTrailer.trailerType,
            purchaseCost: props.semiTrailer.purchaseCost,
            depreciationStartDate: props.semiTrailer.depreciationStartDate,
            depreciationEndDate: props.semiTrailer.depreciationEndDate,
            maintenanceCostPerKm: props.semiTrailer.maintenanceCostPerKm,
        } satisfies SemiTrailerFormData)
    } else {
        Object.assign(form, createEmptySemiTrailerForm())
    }
}

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

const loadTrailerTypes = async () => {
    try {
        trailerTypes.value = await getSemiTrailerTypes()
    } catch (error: unknown) {
        trailerTypes.value = []
        notification.error(
            "Chargement impossible",
            getApiErrorMessage(error, "Les types de semi-remorques n’ont pas pu être chargés."),
        )
    }
}

const submitSemiTrailer = async () => {
    try {
        loading.value = true

        const payload: CreateSemiTrailerRequest = {
            registration: normalizeVehicleRegistration(form.registration),
            trailerType: form.trailerType,
            purchaseCost: form.purchaseCost,
            depreciationStartDate: form.depreciationStartDate || null,
            depreciationEndDate: form.depreciationEndDate || null,
            maintenanceCostPerKm: form.maintenanceCostPerKm,
        }

        if (props.semiTrailer) {
            await updateSemiTrailer(props.semiTrailer.id, payload)

            notification.success(
                "Semi-remorque modifiée",
                `La semi-remorque « ${formatVehicleRegistration(payload.registration)} » a bien été modifiée.`,
            )
            emit("updated")
        } else {
            const semiTrailer = await createSemiTrailer(payload)

            notification.success(
                "Semi-remorque créée",
                `La semi-remorque « ${formatVehicleRegistration(payload.registration)} » a bien été créée.`,
            )
            emit("created", semiTrailer)
        }
    } catch (error: unknown) {
        const action = props.semiTrailer ? "Modification" : "Création"
        const fallback = props.semiTrailer
            ? "La semi-remorque n’a pas pu être modifiée."
            : "La semi-remorque n’a pas pu être créée."

        notification.error(`${action} impossible`, getApiErrorMessage(error, fallback))
    } finally {
        loading.value = false
    }
}

watch(
    [() => props.show, () => props.semiTrailer],
    ([show]) => {
        if (show) {
            syncForm()
            void loadTrailerTypes()
        } else {
            Object.assign(form, createEmptySemiTrailerForm())
            trailerTypes.value = []
        }
    },
    { immediate: true },
)
</script>

<template>
    <AppModal :show="props.show" @close="closeModal">
        <form @submit.prevent="submitSemiTrailer">
            <h2 class="mb-6 text-2xl font-bold text-slate-900">
                {{ props.semiTrailer ? "Modifier la semi-remorque" : "Ajouter une semi-remorque" }}
            </h2>

            <div class="max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar">
                <SemiTrailerForm v-model="form" :disabled="loading" v-model:trailer-types="trailerTypes" />
            </div>

            <div class="mt-6 flex justify-end gap-3">
                <button type="button" :disabled="loading"
                    class="rounded-xl border border-slate-300 px-4 py-2 text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                    @click="closeModal">
                    Annuler
                </button>

                <button type="submit" :disabled="loading"
                    class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                    {{ loading ? "Enregistrement" : "Enregistrer" }}
                </button>
            </div>
        </form>
    </AppModal>
</template>
