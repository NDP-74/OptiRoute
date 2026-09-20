<script setup lang="ts">
import { reactive, ref, watch } from "vue"

import { updateTractor } from "@/api/vehicle/tractorApi"
import { getApiErrorMessage } from "@/api/utils"

import type { TractorDetails, TractorFormData, UpdateTractorRequest } from "@/models/vehicle/Tractor"

import { createEmptyTractorForm } from "@/utils/vehicleUtils"

import AppModal from "@/components/ui/AppModal.vue"
import TractorForm from "@/components/vehicles/TractorForm.vue"

import { useNotification } from "@/composables/useNotification"

//Variables

const notification = useNotification()
const loading = ref(false)

const props = defineProps<{
    show: boolean
    tractor: TractorDetails | null
}>()

const emit = defineEmits<{
    close: []
    updated: []
}>()

//Manage Modal

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

// Manage form

const form = reactive<TractorFormData>(
    createEmptyTractorForm(),
)

const populateForm = () => {
    if (!props.tractor) {
        return
    }

    Object.assign(form, {
        registration: props.tractor.registration,
        brand: props.tractor.brand,
        model: props.tractor.model,

        maxSpeed: props.tractor.maxSpeed,

        fuelType: props.tractor.fuelType ?? null,
        averageConsumption: props.tractor.averageConsumption,
        averageConsumptionEmpty: props.tractor.averageConsumptionEmpty,

        emptyWeightKg: props.tractor.emptyWeightKg,
        grossCombinationWeightKg: props.tractor.grossCombinationWeightKg,

        heightCm: props.tractor.heightCm,
        widthCm: props.tractor.widthCm,
        lengthCm: props.tractor.lengthCm,

        axleCount: props.tractor.axleCount,

        purchaseCost: props.tractor.purchaseCost,
        depreciationStartDate: props.tractor.depreciationStartDate,
        depreciationEndDate: props.tractor.depreciationEndDate,
    } satisfies TractorFormData)
}

const submitTractor = async () => {
    if (!props.tractor) {
        return
    }

    try {
        loading.value = true

        const payload: UpdateTractorRequest = {
            registration: form.registration.trim().toUpperCase(),
            brand: form.brand?.trim() || null,
            model: form.model?.trim() || null,

            maxSpeed: form.maxSpeed,

            fuelType: form.fuelType,
            averageConsumption: form.averageConsumption,
            averageConsumptionEmpty: form.averageConsumptionEmpty,

            emptyWeightKg: form.emptyWeightKg,
            grossCombinationWeightKg: form.grossCombinationWeightKg,

            heightCm: form.heightCm,
            widthCm: form.widthCm,
            lengthCm: form.lengthCm,

            axleCount: form.axleCount,

            purchaseCost: form.purchaseCost,
            depreciationStartDate: form.depreciationStartDate || null,
            depreciationEndDate: form.depreciationEndDate || null,
        }

        await updateTractor(props.tractor.id, payload)

        notification.success(
            "Tracteur modifié",
            `Le tracteur « ${payload.registration} » a bien été modifié.`,
        )

        emit("updated")
    } catch (error: unknown) {
        notification.error(
            "Modification impossible",
            getApiErrorMessage(error, "Le tracteur n’a pas pu être modifié."),
        )
    } finally {
        loading.value = false
    }
}

//Watch

watch(
    [() => props.show, () => props.tractor],
    ([show, tractor]) => {
        if (show && tractor) {
            populateForm()
        }
    },
    {
        immediate: true,
    },
)
</script>

<template>
    <AppModal :show="props.show" @close="closeModal">
        <form @submit.prevent="submitTractor">
            <h2 class="mb-6 text-2xl font-bold text-slate-900">
                Modifier un tracteur
            </h2>

            <div class="max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar">
                <TractorForm v-model="form" :disabled="loading" />
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