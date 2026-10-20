<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { MoreHorizontal, Plus, Search } from "lucide-vue-next"

import { getCostParameters, setCostParameterActive } from "@/api/cost/costParameterApi"

import type { CostParameter, CostParameterLight } from "@/models/cost/CostParameter"

import CreateCostParameterModal from "@/components/cost/CreateCostParameterModal.vue"
import DeleteCostParameterModal from "@/components/cost/DeleteCostParameterModal.vue"
import CostParameterDetailDrawer from "@/components/cost/CostParameterDetailDrawer.vue"

import AppDropdown from "@/components/ui/AppDropdown.vue"
import { categoryLabels, formatUnit, formatValue } from "@/utils/costParameterUtils"

// Load cost parameters

const costParameters = ref<CostParameterLight[]>([])

const loadCostParameters = async () => {
    costParameters.value = await getCostParameters()
}

// Filter by category

const vehicleCosts = computed(() =>
    costParameters.value.filter(
        cost => cost.category === "VEHICLE",
    ),
)

const driverCosts = computed(() =>
    costParameters.value.filter(
        cost => cost.category === "DRIVER",
    ),
)

const structureCosts = computed(() =>
    costParameters.value.filter(
        cost => cost.category === "STRUCTURE",
    ),
)

const selectedCostCategory = ref<"ALL" | "VEHICLE" | "DRIVER" | "STRUCTURE" | "ACTIVE_ONLY">("ALL")
const selectedSort = ref<"CREATION_DATE" | "ALPHABET" | "CATEGORY" | "ACTIVE_FIRST">("CREATION_DATE")
const searchQuery = ref("")

const getCategoryRank = (category: string) => {
    switch (category) {
        case "DRIVER":
            return 0
        case "VEHICLE":
            return 1
        case "STRUCTURE":
            return 2
        default:
            return 3
    }
}

const sortCosts = (costs: CostParameterLight[]) => {
    return [...costs].sort((first, second) => {
        if (selectedSort.value === "ALPHABET") {
            const labelComparison = first.label.localeCompare(second.label, "fr", { sensitivity: "base" })

            if (labelComparison !== 0) {
                return labelComparison
            }

            return first.id - second.id
        }

        if (selectedSort.value === "CATEGORY") {
            const categoryComparison = getCategoryRank(first.category) - getCategoryRank(second.category)

            if (categoryComparison !== 0) {
                return categoryComparison
            }

            return first.id - second.id
        }

        if (selectedSort.value === "ACTIVE_FIRST") {
            if (first.active !== second.active) {
                return Number(second.active) - Number(first.active)
            }

            return first.id - second.id
        }

        return first.id - second.id
    })
}

const visibleCosts = computed(() => {
    const baseCosts = (() => {
        switch (selectedCostCategory.value) {
            case "VEHICLE":
                return vehicleCosts.value
            case "DRIVER":
                return driverCosts.value
            case "STRUCTURE":
                return structureCosts.value
            case "ACTIVE_ONLY":
                return costParameters.value.filter(cost => cost.active)
            default:
                return costParameters.value
        }
    })()

    const sortedBaseCosts = sortCosts(baseCosts)

    if (!searchQuery.value.trim()) {
        return sortedBaseCosts
    }

    const normalized = searchQuery.value.trim().toLowerCase()

    return sortedBaseCosts.filter(cost =>
        cost.label.toLowerCase().includes(normalized)
        || (cost.category === "VEHICLE" && "véhicule".includes(normalized))
        || (cost.category === "DRIVER" && "conducteur".includes(normalized))
        || (cost.category === "STRUCTURE" && "structure".includes(normalized))
        || formatUnit(cost.unit).toLowerCase().includes(normalized)
    )
})

// Manage modals

const showCreateModal = ref(false)
const showDeleteModal = ref(false)
const openDropdownId = ref<number | null>(null)

const actionCostParameter = ref<CostParameterLight | null>(null)
const selectedCostParameterId = ref<number | null>(null)

const askDeleteCostParameter = (
    costParameter: CostParameterLight,
) => {
    actionCostParameter.value = costParameter
    showDeleteModal.value = true
}

const openCostParameterDetails = (
    costParameter: CostParameterLight,
) => {
    selectedCostParameterId.value = costParameter.id

    //TODO: temporaire -> a optimiser ?
    actionCostParameter.value = costParameter
}

const closeCostParameterDetails = () => {
    selectedCostParameterId.value = null
}

const closeCostParameterAction = () => {
    showDeleteModal.value = false
    actionCostParameter.value = null
}

// Handle

const handleCostParameterCreated = async () => {
    showCreateModal.value = false
    await loadCostParameters()
}

const handleCostParameterUpdated = async () => {
    await loadCostParameters()

    if (selectedCostParameterId.value !== null) {
        actionCostParameter.value = costParameters.value.find(
            cost => cost.id === selectedCostParameterId.value,
        ) ?? null
    }
}

const handleCostParameterDeleted = async () => {
    closeCostParameterAction()
    closeCostParameterDetails()
    await loadCostParameters()
}

const toggleCostParameterActive = async (costParameter: CostParameterLight) => {
    try {
        await setCostParameterActive(costParameter.id, !costParameter.active)
        await loadCostParameters()
    } catch (error) {
        console.error("Impossible de modifier le statut du coût :", error)
    }
}

onMounted(loadCostParameters)
</script>

<template>
    <div class="w-full max-w-5xl">
        <!-- Header -->

        <div class="mb-6 flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
                <h1 class="text-2xl font-semibold leading-7 tracking-tight text-gray-900">
                    Coûts référentiels
                </h1>

                <p class="mt-1 text-sm leading-6 text-gray-500">
                    Gérez les coûts utilisés pour les estimations.
                </p>
            </div>

            <button type="button"
                class="inline-flex shrink-0 items-center justify-center gap-2 rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
                @click="showCreateModal = true">
                <Plus class="h-4 w-4" aria-hidden="true" />
                Ajouter un coût
            </button>
        </div>

        <section class="mb-8">
            <div class="mb-4 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">

                <div class="flex w-full flex-col gap-3 sm:flex-row sm:items-end sm:gap-4">
                    <div class="flex w-full flex-col gap-3 sm:max-w-xl sm:flex-row sm:gap-4">
                        <div class="w-full sm:max-w-xs">
                            <label class="mb-1.5 block text-sm font-medium text-gray-700">
                                Filtre
                            </label>

                            <select v-model="selectedCostCategory"
                                class="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 shadow-sm outline-none transition hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20">
                                <option value="ALL">
                                    Tous les coûts
                                </option>

                                <option value="ACTIVE_ONLY">
                                    Coûts activés
                                </option>

                                <option value="DRIVER">
                                    Coûts chauffeur
                                </option>

                                <option value="VEHICLE">
                                    Coûts véhicule
                                </option>

                                <option value="STRUCTURE">
                                    Coûts structure
                                </option>
                            </select>
                        </div>

                        <div class="w-full sm:max-w-xs">
                            <label class="mb-1.5 block text-sm font-medium text-gray-700">
                                Tri
                            </label>

                            <select v-model="selectedSort"
                                class="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 shadow-sm outline-none transition hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20">
                                <option value="CREATION_DATE">
                                    Date de création
                                </option>

                                <option value="ALPHABET">
                                    Alphabet
                                </option>

                                <option value="CATEGORY">
                                    Catégorie
                                </option>

                                <option value="ACTIVE_FIRST">
                                    Actif en premier
                                </option>
                            </select>
                        </div>
                    </div>

                    <div class="w-full sm:ml-auto sm:max-w-xs">
                        <label class="mb-1.5 block text-sm font-medium text-gray-700">
                            Rechercher
                        </label>

                        <div class="relative">
                            <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
                                aria-hidden="true" />
                            <input v-model="searchQuery" type="text" placeholder="Rechercher..."
                                class="w-full rounded-md border border-gray-300 bg-white py-2 pl-9 pr-3 text-sm text-gray-700 shadow-sm outline-none transition placeholder:text-gray-400 hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20" />
                        </div>
                    </div>
                </div>
            </div>

            <div class="max-h-[calc(100vh-21rem)] w-full max-w-5xl overflow-auto rounded-xl border border-gray-200 bg-white shadow-sm">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="sticky top-0 z-10 bg-gray-50">
                        <tr>
                            <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                                Libellé
                            </th>

                            <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                                Valeur
                            </th>

                            <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                                Catégorie
                            </th>

                            <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                                Statut
                            </th>

                            <th class="whitespace-nowrap px-4 py-3 text-right text-sm font-semibold text-gray-700">
                                Actions
                            </th>
                        </tr>
                    </thead>

                    <tbody class="divide-y divide-gray-100">
                        <tr v-for="cost in visibleCosts" :key="cost.id"
                            class="cursor-pointer transition-colors hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-indigo-500" :class="{
                                'bg-indigo-50': selectedCostParameterId === cost.id,
                            }" tabindex="0" @click="openCostParameterDetails(cost)"
                            @keydown.enter="openCostParameterDetails(cost)"
                            @keydown.space.prevent="openCostParameterDetails(cost)">
                            <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                                {{ cost.label }}
                            </td>

                            <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                                {{ formatValue(cost.value, cost.unit) }}
                            </td>

                            <td class="whitespace-nowrap px-4 py-3 text-sm text-slate-600">
                                {{ categoryLabels[cost.category] }}
                            </td>

                            <td class="whitespace-nowrap px-4 py-3">
                                <span class="inline-flex rounded-full px-2.5 py-1 text-xs font-medium" :class="cost.active
                                    ? 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20'
                                    : 'bg-gray-100 text-gray-600 ring-1 ring-inset ring-gray-500/10'">
                                    {{ cost.active ? "Actif" : "Désactivé" }}
                                </span>
                            </td>

                            <td class="whitespace-nowrap px-4 py-3 text-right" @click.stop @keydown.stop>
                                <AppDropdown :open="openDropdownId === cost.id" @update:open="value => {
                                    openDropdownId = value ? cost.id : null
                                }">
                                    <template #trigger="{ toggle }">
                                        <button type="button"
                                            class="rounded-md p-1.5 text-gray-400 transition hover:bg-gray-100 hover:text-gray-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600"
                                            :aria-label="`Actions pour ${cost.label}`" @click.stop="toggle">
                                            <MoreHorizontal class="h-5 w-5" aria-hidden="true" />
                                        </button>
                                    </template>
                                    <template #default="{ close }">
                                        <button type="button"
                                            class="flex w-full items-center px-3 py-2.5 text-left text-sm text-gray-700 transition hover:bg-gray-50 focus:bg-gray-50 focus:outline-none"
                                            @click="close(); void toggleCostParameterActive(cost)">
                                            {{ cost.active ? "Désactiver" : "Activer" }}
                                        </button>

                                        <button type="button"
                                            class="flex w-full items-center px-3 py-2.5 text-left text-sm text-red-600 transition hover:bg-red-50 focus:bg-red-50 focus:outline-none"
                                            @click="close(); askDeleteCostParameter(cost)">
                                            Supprimer
                                        </button>
                                    </template>
                                </AppDropdown>
                            </td>
                        </tr>

                        <tr v-if="visibleCosts.length === 0">
                            <td colspan="5" class="px-4 py-10 text-center text-sm text-gray-500">
                                Aucun coût correspondant à ce filtre.
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <!-- Detail drawer -->

        <CostParameterDetailDrawer :open="selectedCostParameterId !== null" :cost-parameter-id="selectedCostParameterId"
            @close="closeCostParameterDetails" @updated="handleCostParameterUpdated"
            @deleted="handleCostParameterDeleted" />

        <!-- Create -->

        <CreateCostParameterModal :show="showCreateModal" @close="showCreateModal = false"
            @created="handleCostParameterCreated" />

        <!-- Delete -->

        <DeleteCostParameterModal :show="showDeleteModal" :cost-parameter="actionCostParameter"
            @close="closeCostParameterAction" @deleted="handleCostParameterDeleted" />
    </div>
</template>