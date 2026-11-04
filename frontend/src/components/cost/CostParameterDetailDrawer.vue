<script setup lang="ts">
import { onMounted, ref, watch } from "vue"

import { Edit3, Settings2, Trash2, WalletCards, Info, ListChecks } from "lucide-vue-next"

import AppDetailDrawer from "@/components/ui/AppDetailDrawer.vue"
import DetailSection from "@/components/ui/DetailSection.vue"
import DetailRow from "@/components/ui/DetailRow.vue"

import UpdateCostParameterModal from "@/components/cost/UpdateCostParameterModal.vue"
import DeleteCostParameterModal from "@/components/cost/DeleteCostParameterModal.vue"

import type { CostParameter, CostCondition } from "@/models/cost/CostParameter"

import { formatNumber } from "@/utils/formatters"
import { getSemiTrailerTypes } from "@/api/vehicle/semiTrailerApi"
import { getCostParameter } from "@/api/cost/costParameterApi"
import {
    assignmentTypeLabels,
    categoryLabels,
    conditionFieldLabels,
    conditionOperatorLabels,
    conditionSourceLabels,
    formatConditionValue,
    formatUnit,
} from "@/utils/costParameterUtils"

// Props

const props = defineProps<{
    open: boolean
    costParameterId: number | null
}>()

const emit = defineEmits<{
    close: []
    updated: []
    deleted: []
}>()

// Manage Modal

const showUpdateModal = ref(false)
const showDeleteModal = ref(false)
const costParameter = ref<CostParameter | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const trailerTypeLabels = ref<Record<string, string>>({})

const loadTrailerTypeLabels = async () => {
    try {
        const trailerTypes = await getSemiTrailerTypes()
        trailerTypeLabels.value = Object.fromEntries(
            trailerTypes.map((trailerType) => [String(trailerType.id), trailerType.label]),
        )
    } catch {
        trailerTypeLabels.value = {}
    }
}

let currentRequestId = 0

const loadCostParameter = async () => {
    if (props.costParameterId === null) return

    const requestId = ++currentRequestId
    try {
        loading.value = true
        error.value = null
        costParameter.value = null
        const response = await getCostParameter(props.costParameterId)
        if (requestId === currentRequestId) costParameter.value = response.data
    } catch {
        if (requestId === currentRequestId) error.value = "Impossible de charger le coût."
    } finally {
        if (requestId === currentRequestId) loading.value = false
    }
}

onMounted(() => {
    loadTrailerTypeLabels()
})

watch(() => props.open, (open) => {
    if (open) {
        loadTrailerTypeLabels()
        loadCostParameter()
    }
})

watch(() => props.costParameterId, () => {
    if (props.open) loadCostParameter()
})

const openUpdateModal = () => {
    showUpdateModal.value = true
}

const closeUpdateModal = () => {
    showUpdateModal.value = false
}

const openDeleteModal = () => {
    showDeleteModal.value = true
}

const closeDeleteModal = () => {
    showDeleteModal.value = false
}

// Handle

const handleCostParameterUpdated = () => {
    closeUpdateModal()
    loadCostParameter()
    emit("updated")
}

const handleCostParameterDeleted = () => {
    closeDeleteModal()
    emit("deleted")
    emit("close")
}

// Formatting

const formatCondition = (condition: CostCondition) => {
    const field = conditionFieldLabels[condition.field] ?? condition.field

    const operator = conditionOperatorLabels[condition.operator] ?? condition.operator
    const value = formatConditionValue(condition.field, condition.value, trailerTypeLabels.value)

    return `${field} ${operator} ${value}`
}
</script>

<template>
    <AppDetailDrawer :open="props.open" :title="costParameter?.label ?? 'Détails du coût'" @close="emit('close')">
        <template #header>
            <div class="min-w-0">
                <p class="text-xs font-medium uppercase tracking-wide text-blue-600">
                    Coût référentiel
                </p>

                <h2 class="mt-1 truncate text-xl font-bold text-slate-900">
                    {{ costParameter?.label ?? "Détails du coût" }}
                </h2>

                <p v-if="costParameter" class="mt-1 text-sm text-slate-500">
                    {{ categoryLabels[costParameter.category] }}
                </p>
            </div>
        </template>

        <div v-if="loading" class="p-6 text-sm text-slate-500">Chargement des informations...</div>

        <div v-else-if="error" class="p-6 text-sm text-red-700">{{ error }}</div>

        <div v-else-if="costParameter" class="space-y-6 p-6">
            <!-- Informations générales -->

            <DetailSection title="Informations générales" :icon="Info">
                <DetailRow label="Catégorie" :value="categoryLabels[costParameter.category]" />

                <DetailRow label="Libellé" :value="costParameter.label" />

                <DetailRow label="Type d'assignation" :value="assignmentTypeLabels[costParameter.assignmentType]" />

                <DetailRow label="Statut" :value="costParameter.active ? 'Actif' : 'Désactivé'" />
            </DetailSection>

            <!-- Valeur -->

            <DetailSection title="Valeur" :icon="WalletCards">
                <DetailRow label="Coût"
                    :value="`${formatNumber(costParameter.value)} ${formatUnit(costParameter.unit)}`" />
            </DetailSection>

            <!-- Conditions -->

            <DetailSection title="Logique" :icon="Settings2">
                <div class="min-h-[44px] flex items-center px-1">
                    <p class="text-sm leading-5 text-slate-600">
                        <template v-if="!costParameter.rule">
                            Ce coût est toujours applicable.
                        </template>

                        <template v-else-if="costParameter.rule.logicalOperator === 'AND'">
                            Toutes les conditions doivent être respectées.
                        </template>

                        <template v-else>
                            Au moins une condition doit être respectée.
                        </template>
                    </p>
                </div>
            </DetailSection>

            <DetailSection v-if="costParameter.rule" title="Conditions" :icon="ListChecks">
                <div class="space-y-3 py-3">
                    <div v-for="condition in costParameter.rule.conditions" :key="condition.id"
                        class="rounded-xl border border-slate-200 bg-white px-4 py-3">
                        <p class="text-xs font-medium uppercase tracking-wide text-slate-500">
                            {{ conditionSourceLabels[condition.source] ?? condition.source }}
                        </p>

                        <p class="mt-1 text-sm font-medium text-slate-800">
                            {{ formatCondition(condition) }}
                        </p>
                    </div>
                </div>
            </DetailSection>
        </div>

        <!-- Actions -->

        <template v-if="costParameter" #footer>
            <div class="flex items-center justify-between gap-3">
                <button type="button"
                    class="inline-flex items-center gap-2 rounded-xl border border-red-200 px-4 py-2 text-sm font-medium text-red-600 transition hover:bg-red-50"
                    @click="openDeleteModal">
                    <Trash2 class="h-4 w-4" />
                    Supprimer
                </button>

                <button type="button"
                    class="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700"
                    @click="openUpdateModal">
                    <Edit3 class="h-4 w-4" />
                    Modifier
                </button>
            </div>
        </template>
    </AppDetailDrawer>

    <UpdateCostParameterModal :show="showUpdateModal" :cost-parameter="costParameter" @close="closeUpdateModal"
        @updated="handleCostParameterUpdated" />

    <DeleteCostParameterModal :show="showDeleteModal" :cost-parameter="costParameter" @close="closeDeleteModal"
        @deleted="handleCostParameterDeleted" />
</template>