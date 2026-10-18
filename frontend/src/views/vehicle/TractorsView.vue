<script setup lang="ts">
import { onMounted, ref } from "vue"

import { getTractors } from "@/api/vehicle/tractorApi"
import type { TractorSummary } from "@/models/vehicle/Tractor"

import CreateTractorModal from "@/components/vehicles/CreateTractorModal.vue"
import DeleteTractorModal from "@/components/vehicles/DeleteTractorModal.vue"
import TractorDetailDrawer from "@/components/vehicles/TractorDetailDrawer.vue"

import AppDropdown from "@/components/ui/AppDropdown.vue"

//Load tractors

const tractors = ref<TractorSummary[]>([])

const loadTractors = async () => {
    tractors.value = await getTractors()
}

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
    <div>
        <div class="mb-6 flex items-center justify-between">
            <h1 class="text-3xl font-bold text-gray-800">
                Tracteurs
            </h1>

            <button type="button"
                class="rounded-xl bg-blue-600 px-5 py-3 text-white shadow transition hover:bg-blue-700"
                @click="showCreateModal = true">
                Ajouter un tracteur
            </button>
        </div>

        <div class="rounded-2xl bg-white shadow">
            <table class="w-full">
                <thead class="border-b bg-gray-50">
                    <tr>
                        <th class="whitespace-nowrap px-6 py-4 text-left">
                            Immatriculation
                        </th>

                        <th class="whitespace-nowrap px-6 py-4 text-right">
                            Actions
                        </th>
                    </tr>
                </thead>

                <tbody>
                    <tr v-for="tractor in tractors" :key="tractor.id"
                        class="cursor-pointer border-b transition last:border-b-0 hover:bg-gray-100" :class="{
                            'bg-blue-50': selectedTractorId === tractor.id
                        }" tabindex="0" @click="openTractorDetails(tractor)"
                        @keydown.enter="openTractorDetails(tractor)"
                        @keydown.space.prevent="openTractorDetails(tractor)">

                        <td class="whitespace-nowrap px-6 py-4 font-medium text-gray-900">
                            {{ tractor.registration }}
                        </td>

                        <!-- Le stop empêche le clic sur les actions d'ouvrir également le drawer. -->
                        <td class="whitespace-nowrap px-6 py-4 text-right" @click.stop @keydown.stop>
                            <AppDropdown :open="openDropdownId === tractor.id" @update:open="value => {
                                openDropdownId = value ? tractor.id : null
                            }" v-slot="{ close }">
                                <button type="button"
                                    class="flex w-full items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                                    @click="close(); askDeleteTractor(tractor)">
                                    Supprimer
                                </button>
                            </AppDropdown>
                        </td>
                    </tr>

                    <tr v-if="tractors.length === 0">
                        <td colspan="7" class="px-6 py-12 text-center text-sm text-gray-500">
                            Aucun tracteur enregistré.
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