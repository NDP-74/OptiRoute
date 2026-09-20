<script setup lang="ts">
import { reactive, ref, watch } from "vue"

import { updateCostParameter } from "@/api/cost/costParameterApi"
import { getApiErrorMessage } from "@/api/utils"

import type { CostParameter, CostParameterFormData, UpdateCostParameterRequest } from "@/models/cost/CostParameter"

import { createEmptyCostParameterForm } from "@/utils/costParameterUtils"

import AppModal from "@/components/ui/AppModal.vue"
import CostParameterForm from "@/components/cost/CostParameterForm.vue"

import { useNotification } from "@/composables/useNotification"

// Variables

const notification = useNotification()
const loading = ref(false)

const props = defineProps<{
    show: boolean
    costParameter: CostParameter | null
}>()

const emit = defineEmits<{
    close: []
    updated: []
}>()

// Manage Modal

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

// Manage form

const form = reactive<CostParameterFormData>(
    createEmptyCostParameterForm(),
)

const populateForm = () => {
    if (!props.costParameter) {
        return
    }

    Object.assign(form, {
        label: props.costParameter.label,
        category: props.costParameter.category,
        unit: props.costParameter.unit,
        value: props.costParameter.value,
        active: props.costParameter.active,

        rule: props.costParameter.rule ?? null,
    } satisfies CostParameterFormData)
}

const submitCostParameter = async () => {
    if (!props.costParameter) {
        return
    }

    try {
        loading.value = true

        const payload: UpdateCostParameterRequest = {
            label: form.label.trim(),
            category: form.category,
            unit: form.unit,
            value: form.value,
            active: form.active,

            rule: form.rule,
        }

        await updateCostParameter(
            props.costParameter.id,
            payload,
        )

        notification.success(
            "Coût modifié",
            `Le coût « ${payload.label} » a bien été modifié.`,
        )

        emit("updated")
    } catch (error: unknown) {
        notification.error(
            "Modification impossible",
            getApiErrorMessage(
                error,
                "Le coût n’a pas pu être modifié.",
            ),
        )
    } finally {
        loading.value = false
    }
}

// Watch

watch(
    [() => props.show, () => props.costParameter],
    ([show, costParameter]) => {
        if (show && costParameter) {
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

        <form @submit.prevent="submitCostParameter">

            <h2 class="mb-6 text-2xl font-bold text-slate-900">
                Modifier un coût
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
                    {{ loading ? "Enregistrement" : "Enregistrer" }}
                </button>

            </div>

        </form>

    </AppModal>
</template>