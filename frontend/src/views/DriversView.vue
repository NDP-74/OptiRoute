<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { MoreHorizontal, Plus, Search } from "lucide-vue-next"

import { getDrivers } from "@/api/driver/driverApi"

import type { DriverCostType, DriverSummary, DriverType } from "@/models/driver/Driver"

import CreateDriverModal from "@/components/drivers/CreateDriverModal.vue"
import DeleteDriverModal from "@/components/drivers/DeleteDriverModal.vue"
import DriverDetailDrawer from "@/components/drivers/DriverDetailDrawer.vue"
import AppDropdown from "@/components/ui/AppDropdown.vue"

//Load drivers

const drivers = ref<DriverSummary[]>([])

const loadDrivers = async () => {
    drivers.value = await getDrivers()
}

const selectedCostType = ref<DriverCostType | "ALL">("ALL")
const selectedDriverType = ref<DriverType | "ALL">("ALL")
const selectedSort = ref<"NAME" | "DRIVER_TYPE" | "COST_TYPE">("NAME")
const searchQuery = ref("")

const costTypeLabels: Record<DriverCostType, string> = {
    HOURLY: "Horaire",
    FIXED: "Forfaitaire",
}

const driverTypeLabels: Record<DriverType, string> = {
    LONG_HAUL: "Grand routier",
    SHORT_DISTANCE: "Grand régional",
    REGIONAL: "Régional",
}

const normalizeSearchText = (value: string) =>
    value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("fr")

const visibleDrivers = computed(() => {
    const normalizedQuery = normalizeSearchText(searchQuery.value.trim())
    const matchingDrivers = drivers.value.filter(driver => {
        const matchesCostType = selectedCostType.value === "ALL"
            || driver.costType === selectedCostType.value
        const matchesDriverType = selectedDriverType.value === "ALL"
            || driver.driverType === selectedDriverType.value
        const matchesSearch = !normalizedQuery
            || normalizeSearchText(`${driver.firstName} ${driver.lastName}`).includes(normalizedQuery)
            || normalizeSearchText(`${driver.lastName} ${driver.firstName}`).includes(normalizedQuery)

        return matchesCostType && matchesDriverType && matchesSearch
    })

    return matchingDrivers.sort((first, second) => {
        if (selectedSort.value === "DRIVER_TYPE") {
            const typeComparison = driverTypeLabels[first.driverType].localeCompare(
                driverTypeLabels[second.driverType],
                "fr",
                { sensitivity: "base" },
            )
            if (typeComparison !== 0) return typeComparison
        }

        if (selectedSort.value === "COST_TYPE") {
            const typeComparison = costTypeLabels[first.costType].localeCompare(
                costTypeLabels[second.costType],
                "fr",
                { sensitivity: "base" },
            )
            if (typeComparison !== 0) return typeComparison
        }

        return first.lastName.localeCompare(second.lastName, "fr", { sensitivity: "base" })
            || first.firstName.localeCompare(second.firstName, "fr", { sensitivity: "base" })
            || first.id - second.id
    })
})

//Manage Modal

const showCreateModal = ref(false)
const showDeleteModal = ref(false)

const actionDriver = ref<DriverSummary | null>(null)
const selectedDriverId = ref<number | null>(null)
const openDropdownId = ref<number | null>(null)

const askDeleteDriver = (driver: DriverSummary) => {
    actionDriver.value = driver
    showDeleteModal.value = true
}

const openDriverDetails = (driver: DriverSummary) => {
    selectedDriverId.value = driver.id
}

const closeDriverDetails = () => {
    selectedDriverId.value = null
}

const closeDriverAction = () => {
    showDeleteModal.value = false
    actionDriver.value = null
}

// Handle

const handleDriverCreated = async () => {
    showCreateModal.value = false
    await loadDrivers()
}

const handleDriverDeleted = async () => {
    closeDriverAction()
    closeDriverDetails()
    await loadDrivers()
}

onMounted(loadDrivers)
</script>

<template>
    <div class="w-full max-w-5xl">
        <div class="mb-6 flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
                <h1 class="text-2xl font-semibold leading-7 tracking-tight text-gray-900">
                    Conducteurs
                </h1>
                <p class="mt-1 text-sm leading-6 text-gray-500">
                    Consultez et gérez les conducteurs de votre flotte.
                </p>
            </div>

            <button type="button"
                class="inline-flex shrink-0 items-center justify-center gap-2 rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
                @click="showCreateModal = true">
                <Plus class="h-4 w-4" aria-hidden="true" />
                Ajouter un conducteur
            </button>
        </div>

        <div class="mb-4 rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div class="flex w-full flex-col gap-3 sm:flex-row sm:items-end sm:gap-4">
                <div class="flex w-full flex-col gap-3 sm:flex-row sm:gap-4">
                    <div class="w-full sm:max-w-xs">
                        <label for="driver-cost-type-filter" class="mb-1.5 block text-sm font-medium text-gray-700">
                            Type de coût
                        </label>
                        <select id="driver-cost-type-filter" v-model="selectedCostType"
                            class="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 shadow-sm outline-none transition hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20">
                            <option value="ALL">Tous les coûts</option>
                            <option value="HOURLY">Horaire</option>
                            <option value="FIXED">Forfaitaire</option>
                        </select>
                    </div>

                    <div class="w-full sm:max-w-xs">
                        <label for="driver-type-filter" class="mb-1.5 block text-sm font-medium text-gray-700">
                            Type de chauffeur
                        </label>
                        <select id="driver-type-filter" v-model="selectedDriverType"
                            class="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 shadow-sm outline-none transition hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20">
                            <option value="ALL">Tous les types</option>
                            <option value="LONG_HAUL">Grand routier</option>
                            <option value="SHORT_DISTANCE">Grand régional</option>
                            <option value="REGIONAL">Régional</option>
                        </select>
                    </div>
                </div>

                <div class="w-full sm:max-w-xs">
                    <label for="driver-sort" class="mb-1.5 block text-sm font-medium text-gray-700">
                        Trier par
                    </label>
                    <select id="driver-sort" v-model="selectedSort"
                        class="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 shadow-sm outline-none transition hover:border-gray-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20">
                        <option value="NAME">Nom</option>
                        <option value="DRIVER_TYPE">Type de chauffeur</option>
                        <option value="COST_TYPE">Type de coût</option>
                    </select>
                </div>

                <div class="w-full sm:ml-auto sm:max-w-xs">
                    <label for="driver-search" class="mb-1.5 block text-sm font-medium text-gray-700">
                        Rechercher
                    </label>
                    <div class="relative">
                        <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
                            aria-hidden="true" />
                        <input id="driver-search" v-model="searchQuery" type="search"
                            placeholder="Nom ou prénom..."
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
                            Nom
                        </th>

                        <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                            Prénom
                        </th>

                        <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                            Type de chauffeur
                        </th>

                        <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-700">
                            Type de coût
                        </th>

                        <th class="whitespace-nowrap px-4 py-3 text-right text-sm font-semibold text-gray-700">
                            Actions
                        </th>
                    </tr>
                </thead>

                <tbody class="divide-y divide-gray-100">
                    <tr v-for="driver in visibleDrivers" :key="driver.id"
                        class="cursor-pointer transition-colors hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-indigo-500" :class="{
                            'bg-indigo-50': selectedDriverId === driver.id
                        }" tabindex="0" @click="openDriverDetails(driver)" @keydown.enter="openDriverDetails(driver)"
                        @keydown.space.prevent="openDriverDetails(driver)">

                        <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                            {{ driver.lastName }}
                        </td>

                        <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                            {{ driver.firstName }}
                        </td>

                        <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                            {{ driverTypeLabels[driver.driverType] }}
                        </td>

                        <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                            {{ costTypeLabels[driver.costType] }}
                        </td>

                        <!--Empêche le clic sur le menu d'ouvrir également le drawer -->
                        <td class="whitespace-nowrap px-4 py-3 text-right" @click.stop @keydown.stop>
                            <AppDropdown :open="openDropdownId === driver.id" @update:open="value => {
                                openDropdownId = value ? driver.id : null
                            }">
                                <template #trigger="{ toggle }">
                                    <button type="button"
                                        class="rounded-md p-1.5 text-gray-400 transition hover:bg-gray-100 hover:text-gray-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-indigo-600"
                                        :aria-label="`Actions pour ${driver.firstName} ${driver.lastName}`"
                                        @click.stop="toggle">
                                        <MoreHorizontal class="h-5 w-5" aria-hidden="true" />
                                    </button>
                                </template>
                                <template #default="{ close }">
                                    <button type="button"
                                        class="flex w-full items-center px-3 py-2.5 text-left text-sm text-red-600 transition hover:bg-red-50 focus:bg-red-50 focus:outline-none"
                                        @click="close(); askDeleteDriver(driver)">
                                        Supprimer
                                    </button>
                                </template>
                            </AppDropdown>
                        </td>
                    </tr>

                    <tr v-if="visibleDrivers.length === 0">
                        <td colspan="5" class="px-4 py-10 text-center text-sm text-gray-500">
                            {{ drivers.length === 0
                                ? "Aucun conducteur enregistré."
                                : "Aucun conducteur ne correspond à votre recherche ou à vos filtres." }}
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <DriverDetailDrawer :open="selectedDriverId !== null" :driver-id="selectedDriverId" @close="closeDriverDetails"
            @updated="loadDrivers" @deleted="handleDriverDeleted" />

        <CreateDriverModal :show="showCreateModal" @close="showCreateModal = false" @created="handleDriverCreated" />

        <DeleteDriverModal :show="showDeleteModal" :driver="actionDriver" @close="closeDriverAction"
            @deleted="handleDriverDeleted" />
    </div>
</template>