<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { MoreHorizontal, Plus, Search } from "lucide-vue-next"

import { getSemiTrailerTypes, getSemiTrailers } from "@/api/vehicle/semiTrailerApi"
import type { TrailerTypeOption } from "@/api/vehicle/semiTrailerApi"
import type { SemiTrailerSummary } from "@/models/vehicle/SemiTrailer"

import CreateSemiTrailerModal from "@/components/vehicles/CreateSemiTrailerModal.vue"
import DeleteSemiTrailerModal from "@/components/vehicles/DeleteSemiTrailerModal.vue"
import SemiTrailerDetailDrawer from "@/components/vehicles/SemiTrailerDetailDrawer.vue"

import AppDropdown from "@/components/ui/AppDropdown.vue"
import { formatDate } from "@/utils/formatters"

//Load semi-trailers

const semiTrailers = ref<SemiTrailerSummary[]>([])

const loadSemiTrailers = async () => {
    semiTrailers.value = await getSemiTrailers()
}

const searchQuery = ref("")
const selectedSort = ref<"REGISTRATION" | "DEPRECIATION_ASC" | "DEPRECIATION_DESC">("REGISTRATION")

const visibleSemiTrailers = computed(() => {
    const normalizedQuery = searchQuery.value.trim().toLocaleLowerCase("fr")
    const matchingSemiTrailers = semiTrailers.value.filter(semiTrailer =>
        semiTrailer.registration.toLocaleLowerCase("fr").includes(normalizedQuery),
    )

    return matchingSemiTrailers.sort((first, second) => {
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

const trailerTypes = ref<TrailerTypeOption[]>([])

const loadTrailerTypes = async () => {
    trailerTypes.value = await getSemiTrailerTypes()
}

//Manage Modal

const showCreateModal = ref(false)
const showDeleteModal = ref(false)

const actionSemiTrailer = ref<SemiTrailerSummary | null>(null)
const selectedSemiTrailerId = ref<number | null>(null)
const selectedTrailerTypeId = ref<number | null>(null)
const openActionMenuId = ref<string | null>(null)

const askDeleteSemiTrailer = (semiTrailer: SemiTrailerSummary) => {
    selectedSemiTrailerId.value = null
    selectedTrailerTypeId.value = null
    openActionMenuId.value = null
    actionSemiTrailer.value = semiTrailer
    showDeleteModal.value = true
}

const updateActionMenu = (id: string, open: boolean) => {
    if (open) {
        selectedSemiTrailerId.value = null
        selectedTrailerTypeId.value = null
    }

    openActionMenuId.value = open ? id : null
}

const openSemiTrailerDetails = (semiTrailer: SemiTrailerSummary) => {
    selectedTrailerTypeId.value = null
    openActionMenuId.value = null
    selectedSemiTrailerId.value = semiTrailer.id
}

const closeSemiTrailerDetails = () => {
    selectedSemiTrailerId.value = null
}

const closeSemiTrailerAction = () => {
    showDeleteModal.value = false
    actionSemiTrailer.value = null
}

//Handle

const handleSemiTrailerCreated = async () => {
    showCreateModal.value = false
    await loadSemiTrailers()
}

const handleSemiTrailerDeleted = async () => {
    closeSemiTrailerAction()
    selectedSemiTrailerId.value = null
    await loadSemiTrailers()
}

onMounted(() => {
    loadSemiTrailers()
    loadTrailerTypes()
})
</script>

<template>
    <div class="w-full max-w-5xl">
        <div class="mb-6 flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
                <h1 class="text-2xl font-semibold leading-7 tracking-tight text-gray-900">
                    Semi-remorques
                </h1>
                <p class="mt-1 text-sm leading-6 text-gray-500">
                    Consultez et gérez votre flotte de semi-remorques.
                </p>
            </div>

            <button type="button"
                class="inline-flex shrink-0 items-center justify-center gap-2 rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
                @click="openActionMenuId = null; selectedSemiTrailerId = null; selectedTrailerTypeId = null; showCreateModal = true">
                <Plus class="h-4 w-4" aria-hidden="true" />
                Ajouter un semi-remorque
            </button>
        </div>

        <div class="mb-4 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div class="flex w-full flex-col gap-3 sm:flex-row sm:items-end sm:gap-4">
                <div class="w-full sm:max-w-xs">
                    <label for="semi-trailer-sort" class="mb-1.5 block text-sm font-medium text-gray-700">
                        Trier par
                    </label>
                    <select id="semi-trailer-sort" v-model="selectedSort"
                        class="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 shadow-sm outline-none transition hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20">
                        <option value="REGISTRATION">Immatriculation (A à Z)</option>
                        <option value="DEPRECIATION_ASC">Fin d’amortissement (croissante)</option>
                        <option value="DEPRECIATION_DESC">Fin d’amortissement (décroissante)</option>
                    </select>
                </div>

                <div class="w-full sm:ml-auto sm:max-w-xs">
                    <label for="semi-trailer-search" class="mb-1.5 block text-sm font-medium text-gray-700">
                        Rechercher
                    </label>
                    <div class="relative">
                        <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
                            aria-hidden="true" />
                        <input id="semi-trailer-search" v-model="searchQuery" type="search"
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
                    <tr v-for="semiTrailer in visibleSemiTrailers" :key="semiTrailer.id"
                        class="cursor-pointer transition-colors hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-indigo-500" :class="{
                            'bg-indigo-50': selectedSemiTrailerId === semiTrailer.id
                        }" tabindex="0" @click="openSemiTrailerDetails(semiTrailer)"
                        @keydown.enter="openSemiTrailerDetails(semiTrailer)"
                        @keydown.space.prevent="openSemiTrailerDetails(semiTrailer)">

                        <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                            {{ semiTrailer.registration }}
                        </td>

                        <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                            {{ formatDate(semiTrailer.depreciationStartDate) }}
                        </td>

                        <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                            {{ formatDate(semiTrailer.depreciationEndDate) }}
                        </td>

                        <!-- Le stop empêche le clic sur les actions d'ouvrir également le drawer. -->
                        <td class="whitespace-nowrap px-4 py-3 text-right" @click.stop @keydown.stop>
                            <AppDropdown :open="openActionMenuId === `semi-${semiTrailer.id}`"
                                @update:open="value => updateActionMenu(`semi-${semiTrailer.id}`, value)"
                            >
                                <template #trigger="{ toggle }">
                                    <button type="button"
                                        class="rounded-md p-1.5 text-gray-400 transition hover:bg-gray-100 hover:text-gray-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600"
                                        :aria-label="`Actions pour le semi-remorque ${semiTrailer.registration}`"
                                        @click.stop="toggle">
                                        <MoreHorizontal class="h-5 w-5" aria-hidden="true" />
                                    </button>
                                </template>
                                <template #default="{ close }">
                                    <button type="button"
                                        class="flex w-full items-center px-3 py-2.5 text-left text-sm text-red-600 transition hover:bg-red-50 focus:bg-red-50 focus:outline-none"
                                        @click="
                                            close(); askDeleteSemiTrailer(semiTrailer)
                                            ">
                                        Supprimer
                                    </button>
                                </template>
                            </AppDropdown>
                        </td>
                    </tr>

                    <tr v-if="visibleSemiTrailers.length === 0">
                        <td colspan="4" class="px-4 py-10 text-center text-sm text-gray-500">
                            {{ semiTrailers.length === 0
                                ? "Aucun semi-remorque enregistré."
                                : "Aucun semi-remorque ne correspond à votre recherche." }}
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <SemiTrailerDetailDrawer :open="selectedSemiTrailerId !== null" :semiTrailerId="selectedSemiTrailerId"
            @close="closeSemiTrailerDetails" @updated="loadSemiTrailers" @deleted="handleSemiTrailerDeleted" />

        <CreateSemiTrailerModal :show="showCreateModal" @close="showCreateModal = false"
            @created="handleSemiTrailerCreated" />

        <DeleteSemiTrailerModal :show="showDeleteModal" :semiTrailer="actionSemiTrailer" @close="closeSemiTrailerAction"
            @deleted="handleSemiTrailerDeleted" />

    </div>
</template>