<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { MoreHorizontal, Plus, Search } from "lucide-vue-next"

import { getTractors } from "@/api/vehicle/tractorApi"
import type { TractorSummary } from "@/models/vehicle/Tractor"

import CreateTractorModal from "@/components/vehicles/CreateTractorModal.vue"
import DeleteTractorModal from "@/components/vehicles/DeleteTractorModal.vue"
import TractorDetailDrawer from "@/components/vehicles/TractorDetailDrawer.vue"

import AppDropdown from "@/components/ui/AppDropdown.vue"
import { formatDate } from "@/utils/formatters"

//Load tractors

const tractors = ref<TractorSummary[]>([])

const loadTractors = async () => {
    tractors.value = await getTractors()
}

const searchQuery = ref("")
const selectedSort = ref<"REGISTRATION" | "DEPRECIATION_ASC" | "DEPRECIATION_DESC">("REGISTRATION")

const visibleTractors = computed(() => {
    const normalizedQuery = searchQuery.value.trim().toLocaleLowerCase("fr")
    const matchingTractors = tractors.value.filter(tractor =>
        tractor.registration.toLocaleLowerCase("fr").includes(normalizedQuery),
    )

    return matchingTractors.sort((first, second) => {
        if (selectedSort.value !== "REGISTRATION") {
            const firstDate = first.depreciationEndDate
            const secondDate = second.depreciationEndDate

            if (firstDate === null && secondDate !== null) return 1
            if (firstDate !== null && secondDate === null) return -1

            if (firstDate !== null && secondDate !== null) {
                const dateComparison = firstDate.localeCompare(secondDate)
                if (dateComparison !== 0) {
                    return selectedSort.value === "DEPRECIATION_ASC"
                        ? dateComparison
                        : -dateComparison
                }
            }
        }

        return first.registration.localeCompare(second.registration, "fr", { sensitivity: "base" })
            || first.id - second.id
    })
})

//Manage Modal

const showCreateModal = ref(false)
const showDeleteModal = ref(false)

const actionTractor = ref<TractorSummary | null>(null)
const selectedTractorId = ref<number | null>(null)
const openDropdownId = ref<number | null>(null)

const askDeleteTractor = (tractor: TractorSummary) => {
    actionTractor.value = tractor
    showDeleteModal.value = true
}

const openTractorDetails = (tractor: TractorSummary) => {
    selectedTractorId.value = tractor.id
}

const closeTractorDetails = () => {
    selectedTractorId.value = null
}

const closeTractorAction = () => {
    showDeleteModal.value = false
    actionTractor.value = null
}

//Handle

const handleTractorCreated = async () => {
    showCreateModal.value = false
    await loadTractors()
}

const handleTractorDeleted = async () => {
    closeTractorAction()
    selectedTractorId.value = null
    await loadTractors()
}

onMounted(loadTractors)
</script>

<template>
    <div class="w-full max-w-5xl">
        <div class="mb-6 flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
                <h1 class="text-2xl font-semibold leading-7 tracking-tight text-gray-900">
                    Tracteurs
                </h1>
                <p class="mt-1 text-sm leading-6 text-gray-500">
                    Consultez et gérez votre flotte de tracteurs.
                </p>
            </div>

            <button type="button"
                class="inline-flex shrink-0 items-center justify-center gap-2 rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
                @click="showCreateModal = true">
                <Plus class="h-4 w-4" aria-hidden="true" />
                Ajouter un tracteur
            </button>
        </div>

        <div class="mb-4 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div class="flex w-full flex-col gap-3 sm:flex-row sm:items-end sm:gap-4">
                <div class="w-full sm:max-w-xs">
                    <label for="tractor-sort" class="mb-1.5 block text-sm font-medium text-gray-700">
                        Trier par
                    </label>
                    <select id="tractor-sort" v-model="selectedSort"
                        class="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 shadow-sm outline-none transition hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20">
                        <option value="REGISTRATION">Immatriculation (A à Z)</option>
                        <option value="DEPRECIATION_ASC">Fin d’amortissement (croissante)</option>
                        <option value="DEPRECIATION_DESC">Fin d’amortissement (décroissante)</option>
                    </select>
                </div>

                <div class="w-full sm:ml-auto sm:max-w-xs">
                    <label for="tractor-search" class="mb-1.5 block text-sm font-medium text-gray-700">
                        Rechercher
                    </label>
                    <div class="relative">
                        <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
                            aria-hidden="true" />
                        <input id="tractor-search" v-model="searchQuery" type="search"
                            placeholder="Immatriculation..."
                            class="w-full rounded-md border border-gray-300 bg-white py-2 pl-9 pr-3 text-sm text-gray-700 shadow-sm outline-none transition placeholder:text-gray-400 hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20" />
                    </div>
                </div>
            </div>
        </div>

        <div class="max-h-[calc(100vh-20rem)] w-full max-w-5xl overflow-auto rounded-xl border border-gray-200 bg-white shadow-sm">
            <table class="min-w-full divide-y divide-gray-200">
                <thead class="sticky top-0 z-10 bg-gray-50">
                    <tr>
                        <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                            Immatriculation
                        </th>

                        <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                            Début d’amortissement
                        </th>

                        <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                            Fin d’amortissement
                        </th>

                        <th class="whitespace-nowrap px-4 py-3 text-right text-sm font-semibold text-gray-700">
                            Actions
                        </th>
                    </tr>
                </thead>

                <tbody class="divide-y divide-gray-100">
                    <tr v-for="tractor in visibleTractors" :key="tractor.id"
                        class="cursor-pointer transition-colors hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-indigo-500" :class="{
                            'bg-indigo-50': selectedTractorId === tractor.id
                        }" tabindex="0" @click="openTractorDetails(tractor)"
                        @keydown.enter="openTractorDetails(tractor)"
                        @keydown.space.prevent="openTractorDetails(tractor)">

                        <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                            {{ tractor.registration }}
                        </td>

                        <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                            {{ formatDate(tractor.depreciationStartDate) }}
                        </td>

                        <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                            {{ formatDate(tractor.depreciationEndDate) }}
                        </td>

                        <!-- Le stop empêche le clic sur les actions d'ouvrir également le drawer. -->
                        <td class="whitespace-nowrap px-4 py-3 text-right" @click.stop @keydown.stop>
                            <AppDropdown :open="openDropdownId === tractor.id" @update:open="value => {
                                openDropdownId = value ? tractor.id : null
                            }">
                                <template #trigger="{ toggle }">
                                    <button type="button"
                                        class="rounded-md p-1.5 text-gray-400 transition hover:bg-gray-100 hover:text-gray-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600"
                                        :aria-label="`Actions pour le tracteur ${tractor.registration}`"
                                        @click.stop="toggle">
                                        <MoreHorizontal class="h-5 w-5" aria-hidden="true" />
                                    </button>
                                </template>
                                <template #default="{ close }">
                                    <button type="button"
                                        class="flex w-full items-center px-3 py-2.5 text-left text-sm text-red-600 transition hover:bg-red-50 focus:bg-red-50 focus:outline-none"
                                        @click="close(); askDeleteTractor(tractor)">
                                        Supprimer
                                    </button>
                                </template>
                            </AppDropdown>
                        </td>
                    </tr>

                    <tr v-if="visibleTractors.length === 0">
                        <td colspan="4" class="px-4 py-10 text-center text-sm text-gray-500">
                            {{ tractors.length === 0
                                ? "Aucun tracteur enregistré."
                                : "Aucun tracteur ne correspond à votre recherche." }}
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <TractorDetailDrawer :open="selectedTractorId !== null" :tractor-id="selectedTractorId"
            @close="closeTractorDetails" @updated="loadTractors" @deleted="handleTractorDeleted" />

        <CreateTractorModal :show="showCreateModal" @close="showCreateModal = false" @created="handleTractorCreated" />

        <DeleteTractorModal :show="showDeleteModal" :tractor="actionTractor" @close="closeTractorAction"
            @deleted="handleTractorDeleted" />
    </div>
</template>