<script setup lang="ts">
import { reactive, ref, watch } from "vue"

import { createSemiTrailer, getSemiTrailerTypes } from "@/api/vehicle/semiTrailerApi"
import { getApiErrorMessage } from "@/api/utils"

import type { CreateSemiTrailerRequest, SemiTrailerFormData } from "@/models/vehicle/SemiTrailer"
import { createEmptySemiTrailerForm } from "@/utils/vehicleUtils"

import AppModal from "@/components/ui/AppModal.vue"
import SemiTrailerForm from "@/components/vehicles/SemiTrailerForm.vue"

import { useNotification } from "@/composables/useNotification"

//Variables

const notification = useNotification()
const loading = ref(false)
const trailerTypes = ref<string[]>([])

const props = defineProps<{
    show: boolean
}>()

const emit = defineEmits<{
    close: []
    created: []
}>()

//Manage Modal

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

// Manage form

const form = reactive<SemiTrailerFormData>(createEmptySemiTrailerForm())

const resetForm = () => {
    Object.assign(form, createEmptySemiTrailerForm())
}

const loadTrailerTypes = async () => {
    try {
        const trailerTypeOptions = await getSemiTrailerTypes()
        trailerTypes.value = trailerTypeOptions.map((option) => option.label)
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
            externalSource: "MANUAL",
            externalId: null,

            registration: form.registration.trim().toUpperCase(),
            brand: form.brand?.trim() || null,
            model: form.model?.trim() || null,

            maxSpeed: form.maxSpeed,
            trailerType: form.trailerType,

            emptyWeightKg: form.emptyWeightKg,
            grossVehicleWeightKg: form.grossVehicleWeightKg,

            heightCm: form.heightCm,
            widthCm: form.widthCm,
            lengthCm: form.lengthCm,

            axleCount: form.axleCount,

            purchaseCost: form.purchaseCost,
            depreciationStartDate: form.depreciationStartDate || null,
            depreciationEndDate: form.depreciationEndDate || null,
        }

        await createSemiTrailer(payload)

        notification.success(
            "Semi-remorque créée",
            `La semi-remorque « ${payload.registration} » a bien été créée.`,
        )

        emit("created")
    } catch (error: unknown) {
        notification.error(
            "Création impossible",
            getApiErrorMessage(error, "La semi-remorque n’a pas pu être créée."),
        )
    } finally {
        loading.value = false
    }
}

//Watch

watch(
    () => props.show,
    (show) => {
        if (show) {
            resetForm()
            loadTrailerTypes()
        }
    },
)
</script>

<template>
    <AppModal :show="props.show" @close="closeModal">
        <form @submit.prevent="submitSemiTrailer">
            <h2 class="mb-6 text-2xl font-bold text-slate-900">
                Ajouter une semi-remorque
            </h2>

            <div class="max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar">
                <SemiTrailerForm v-model="form" :disabled="loading" :trailer-types="trailerTypes" />
            </div>

            <div class="mt-6 flex justify-end gap-3">
                <button type="button" :disabled="loading"
                    class="rounded-xl border border-slate-300 px-4 py-2 text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                    @click="closeModal">
                    Annuler
                </button>

                <button type="submit" :disabled="loading"
                    class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                    {{ loading ? 'Enregistrement' : 'Enregistrer' }}
                </button>
            </div>
        </form>
    </AppModal>
</template>