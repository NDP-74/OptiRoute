<script setup lang="ts">
import { reactive, ref, watch } from "vue"


import { createEmptyCostParameterForm } from "@/utils/costParameterUtils"
import type { CreateCostParameterRequest, CostParameterFormData } from "@/models/cost/CostParameter"

import { getApiErrorMessage } from "@/api/utils"
import { createCostParameter } from "@/api/cost/costParameterApi"

import AppModal from "@/components/ui/AppModal.vue"
import CostParameterForm from "./CostParameterForm.vue"

import { useNotification } from "@/composables/useNotification"

// Variables

const notification = useNotification()
const loading = ref(false)

const props = defineProps<{
    show: boolean
}>()

const emit = defineEmits<{
    close: []
    created: []
}>()

// Form

const form = reactive<CostParameterFormData>(
    createEmptyCostParameterForm(),
)

const resetForm = () => {
    Object.assign(form, createEmptyCostParameterForm())
}

// Modal

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

// Submit

const submitCostParameter = async () => {
    try {
        loading.value = true

        const payload: CreateCostParameterRequest = {
            category: form.category,
            label: form.label.trim(),
            value: form.value,
            unit: form.unit,
            active: form.active,
            rule: form.rule,
        }

        await createCostParameter(payload)

        notification.success(
            "Coût créé",
            `Le coût « ${form.label} » a bien été ajouté.`,
        )

        emit("created")
    } catch (error: unknown) {
        notification.error(
            "Création impossible",
            getApiErrorMessage(
                error,
                "Le coût n’a pas pu être créé.",
            ),
        )
    } finally {
        loading.value = false
    }
}

// Watch

watch(
    () => props.show,
    (show) => {
        if (show) {
            resetForm()
        }
    },
)
</script>

<template>
    <AppModal :show="props.show" @close="closeModal">
        <form @submit.prevent="submitCostParameter">

            <h2 class="mb-6 text-2xl font-bold text-slate-900">
                Ajouter un coût
            </h2>

            <div class="max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar">
                <CostParameterForm v-model="form" :disabled="loading" />
            </div>

            <div class="mt-6 flex justify-end gap-3">
                <button type="button" :disabled="loading"
                    class="rounded-xl border border-slate-300 px-4 py-2 text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                    @click="closeModal">
                    Annuler
                </button>

                <button type="submit" :disabled="loading"
                    class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                    {{ loading ? "Enregistrement..." : "Enregistrer" }}
                </button>
            </div>

        </form>
    </AppModal>
</template>