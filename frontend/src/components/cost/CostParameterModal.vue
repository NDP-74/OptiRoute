<script setup lang="ts">
import { reactive, ref, watch } from "vue"

import { createCostParameter, updateCostParameter } from "@/api/cost/costParameterApi"
import { getApiErrorMessage } from "@/api/utils"

import type { CostParameter, CostParameterFormData, CreateCostParameterRequest } from "@/models/cost/CostParameter"
import { createEmptyCostParameterForm } from "@/utils/costParameterUtils"

import AppModal from "@/components/ui/AppModal.vue"
import CostParameterForm from "@/components/cost/CostParameterForm.vue"

import { useNotification } from "@/composables/useNotification"

const notification = useNotification()
const loading = ref(false)

const props = defineProps<{
    show: boolean
    costParameter?: CostParameter | null
}>()

const emit = defineEmits<{
    close: []
    created: []
    updated: []
}>()

const form = reactive<CostParameterFormData>(createEmptyCostParameterForm())

const syncForm = () => {
    if (props.costParameter) {
        Object.assign(form, {
            label: props.costParameter.label,
            category: props.costParameter.category,
            unit: props.costParameter.unit,
            value: props.costParameter.value,
            active: props.costParameter.active,
            assignmentType: props.costParameter.assignmentType,
            conditions: props.costParameter.conditions.map(condition => ({ ...condition })),
        } satisfies CostParameterFormData)
    } else {
        Object.assign(form, createEmptyCostParameterForm())
    }
}

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

const submitCostParameter = async () => {
    try {
        loading.value = true

        const payload: CreateCostParameterRequest = {
            category: form.category,
            label: form.label.trim(),
            value: form.value,
            unit: form.unit,
            active: form.active,
            assignmentType: form.assignmentType,
            conditions: form.conditions,
        }

        if (props.costParameter) {
            await updateCostParameter(props.costParameter.id, payload)

            notification.success(
                "Coût modifié",
                `Le coût « ${payload.label} » a bien été modifié.`,
            )
            emit("updated")
        } else {
            await createCostParameter(payload)

            notification.success(
                "Coût créé",
                `Le coût « ${payload.label} » a bien été ajouté.`,
            )
            emit("created")
        }
    } catch (error: unknown) {
        const action = props.costParameter ? "Modification" : "Création"
        const fallback = props.costParameter
            ? "Le coût n’a pas pu être modifié."
            : "Le coût n’a pas pu être créé."

        notification.error(
            `${action} impossible`,
            getApiErrorMessage(error, fallback),
        )
    } finally {
        loading.value = false
    }
}

watch(
    [() => props.show, () => props.costParameter],
    ([show]) => {
        if (show) {
            syncForm()
        } else {
            Object.assign(form, createEmptyCostParameterForm())
        }
    },
    { immediate: true },
)
</script>

<template>
    <AppModal :show="props.show" @close="closeModal">
        <form @submit.prevent="submitCostParameter">
            <h2 class="mb-6 text-2xl font-bold text-slate-900">
                {{ props.costParameter ? "Modifier un coût" : "Ajouter un coût" }}
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
