<script setup lang="ts">
import { reactive, ref, watch } from "vue"

import { createTractor, updateTractor } from "@/api/vehicle/tractorApi"
import { getApiErrorMessage } from "@/api/utils"

import type { CreateTractorRequest, TractorDetails, TractorFormData, TractorSummary } from "@/models/vehicle/Tractor"
import { createEmptyTractorForm, formatVehicleRegistration, normalizeVehicleRegistration } from "@/utils/vehicleUtils"

import AppModal from "@/components/ui/AppModal.vue"
import TractorForm from "@/components/vehicles/TractorForm.vue"

import { useNotification } from "@/composables/useNotification"

const notification = useNotification()
const loading = ref(false)

const props = defineProps<{
    show: boolean
    tractor?: TractorDetails | null
}>()

const emit = defineEmits<{
    close: []
    created: [tractor: TractorSummary]
    updated: []
}>()

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

const form = reactive<TractorFormData>(createEmptyTractorForm())

const syncForm = () => {
    if (props.tractor) {
        Object.assign(form, {
            registration: normalizeVehicleRegistration(props.tractor.registration),
            purchaseCost: props.tractor.purchaseCost,
            depreciationStartDate: props.tractor.depreciationStartDate,
            depreciationEndDate: props.tractor.depreciationEndDate,
            maintenanceCostPerKm: props.tractor.maintenanceCostPerKm,
        } satisfies TractorFormData)
    } else {
        Object.assign(form, createEmptyTractorForm())
    }
}

const submitTractor = async () => {
    try {
        loading.value = true

        const payload: CreateTractorRequest = {
            registration: normalizeVehicleRegistration(form.registration),
            purchaseCost: form.purchaseCost,
            depreciationStartDate: form.depreciationStartDate || null,
            depreciationEndDate: form.depreciationEndDate || null,
            maintenanceCostPerKm: form.maintenanceCostPerKm,
        }

        if (props.tractor) {
            await updateTractor(props.tractor.id, payload)

            notification.success(
                "Tracteur modifié",
                `Le tracteur « ${formatVehicleRegistration(payload.registration)} » a bien été modifié.`,
            )
            emit("updated")
        } else {
            const tractor = await createTractor(payload)

            notification.success(
                "Tracteur créé",
                `Le tracteur « ${formatVehicleRegistration(payload.registration)} » a bien été créé.`,
            )
            emit("created", tractor)
        }
    } catch (error: unknown) {
        const action = props.tractor ? "Modification" : "Création"
        const fallback = props.tractor
            ? "Le tracteur n’a pas pu être modifié."
            : "Le tracteur n’a pas pu être créé."

        notification.error(
            `${action} impossible`,
            getApiErrorMessage(error, fallback),
        )
    } finally {
        loading.value = false
    }
}

watch(
    [() => props.show, () => props.tractor],
    ([show]) => {
        if (show) {
            syncForm()
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
                {{ props.tractor ? "Modifier un tracteur" : "Ajouter un tracteur" }}
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
