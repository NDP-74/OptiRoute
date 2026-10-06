<script setup lang="ts">
import { ref, watch } from "vue"

import {
    CircleDollarSign,
    Fuel,
    Gauge,
    Info,
    Ruler,
    Scale,
    Trash2,
    Edit3,
    Tag,
} from "lucide-vue-next"

import type { TractorDetails } from "@/models/vehicle/Tractor"

import { getTractor } from "@/api/vehicle/tractorApi"
import { getApiErrorMessage } from "@/api/utils"


import AppDetailDrawer from "@/components/ui/AppDetailDrawer.vue"
import DetailSection from "@/components/ui/DetailSection.vue"
import DetailRow from "@/components/ui/DetailRow.vue"
import UpdateTractorModal from "@/components/vehicles/UpdateTractorModal.vue"
import DeleteTractorModal from "@/components/vehicles/DeleteTractorModal.vue"

import {
    formatCurrency, formatDate, formatDateTime, formatNumber,
} from "@/utils/formatters"
import { formatFuelType, formatConsumption, formatVehicleLabel } from "@/utils/vehicleUtils"

//Variables

const props = defineProps<{
    open: boolean
    tractorId: number | null
}>()

const emit = defineEmits<{
    close: []
    deleted: []
    updated: []
}>()

const tractor = ref<TractorDetails | null>(null)

const loading = ref(false)
const error = ref<string | null>(null)

/*
 * Permet d’ignorer une ancienne réponse HTTP lorsque
 * l’utilisateur sélectionne rapidement plusieurs tracteurs.
 */
let currentRequestId = 0

const loadTractor = async () => {
    if (props.tractorId === null) {
        return
    }

    const requestId = ++currentRequestId

    loading.value = true
    error.value = null
    tractor.value = null

    try {
        const loadedTractor = await getTractor(props.tractorId)

        if (requestId === currentRequestId) {
            tractor.value = loadedTractor
        }
    } catch (err) {
        if (requestId !== currentRequestId) {
            return
        }

        error.value = getApiErrorMessage(err, "Impossible de charger les informations de la semi-remorque.")
    } finally {
        if (requestId === currentRequestId) {
            loading.value = false
        }
    }
}

//Manage Modal

const showDeleteModal = ref(false)
const showUpdateModal = ref(false)

const openUpdateModal = () => {
    if (!tractor.value) {
        return
    }

    showUpdateModal.value = true
}

const closeUpdateModal = () => {
    showUpdateModal.value = false
}

const openDeleteModal = () => {
    if (!tractor.value) {
        return
    }

    showDeleteModal.value = true
}

const closeDeleteModal = () => {
    showDeleteModal.value = false
}

//Handle

const handleTractorUpdated = async () => {
    showUpdateModal.value = false

    await loadTractor()
    emit("updated")
}

const handleTractorDeleted = () => {
    showDeleteModal.value = false
    tractor.value = null

    emit("deleted")
    emit("close")
}

//Watch

watch(
    [() => props.open, () => props.tractorId],
    ([open, tractorId]) => {
        if (open && tractorId !== null) {
            loadTractor()
            return
        }

        /* Invalide la requête en cours lors de la fermeture. */
        currentRequestId++

        tractor.value = null
        error.value = null
        loading.value = false

        showUpdateModal.value = false
        showDeleteModal.value = false
    },
    {
        immediate: true,
    },
)

</script>

<template>
    <AppDetailDrawer :open="props.open" :title="tractor?.registration ?? 'Détail du tracteur'" @close="emit('close')">
        <template #header>
            <div class="min-w-0">
                <p class="text-xs font-medium uppercase tracking-wide text-blue-600">
                    Détails du tracteur
                </p>

                <h2 class="mt-1 truncate text-xl font-bold text-slate-900">
                    {{ tractor?.registration ?? "Chargement..." }}
                </h2>

                <p v-if="tractor" class="mt-1 truncate text-sm text-slate-500">
                    {{ formatVehicleLabel(tractor?.brand, tractor?.model) }}
                </p>
            </div>
        </template>

        <!-- Chargement -->
        <div v-if="loading" class="flex min-h-full items-center justify-center">
            <div class="text-center">
                <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-slate-200 border-t-blue-600" />

                <p class="mt-3 text-sm text-slate-500">
                    Chargement des informations...
                </p>
            </div>
        </div>

        <!-- Erreur -->
        <div v-else-if="error" class="flex min-h-full items-center justify-center p-6">
            <div class="w-full rounded-2xl border border-red-200 bg-red-50 p-5 text-center">
                <p class="font-medium text-red-800">
                    Chargement impossible
                </p>

                <p class="mt-2 text-sm text-red-700">
                    {{ error }}
                </p>

                <button type="button"
                    class="mt-4 rounded-xl bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700"
                    @click="loadTractor">
                    Réessayer
                </button>
            </div>
        </div>

        <!-- Contenu -->
        <div v-else-if="tractor" class="space-y-6 p-6">
            <!-- Identification -->
            <DetailSection title="Identification" :icon="Info">
                <DetailRow label="Immatriculation" :value="tractor.registration" />

                <DetailRow label="Marque" :value="tractor.brand || 'Non renseignée'" />

                <DetailRow label="Modèle" :value="tractor.model || 'Non renseigné'" />
            </DetailSection>

            <!-- Motorisation -->
            <DetailSection title="Motorisation" :icon="Fuel">
                <DetailRow label="Type de carburant" :value="formatFuelType(tractor.fuelType)" />

                <DetailRow label="Consommation moyenne en charge"
                    :value="formatConsumption(tractor.averageConsumption)" />

                <DetailRow label="Consommation moyenne à vide"
                    :value="formatConsumption(tractor.averageConsumptionEmpty)" />

                <DetailRow label="Vitesse maximale" :value="formatNumber(tractor.maxSpeed, ' km/h', 0)" />
            </DetailSection>

            <!-- Configuration -->
            <DetailSection title="Configuration" :icon="Gauge">
                <DetailRow label="Nombre d’essieux" :value="formatNumber(tractor.axleCount, '', 0)" />
            </DetailSection>

            <!-- Poids -->
            <DetailSection title="Poids" :icon="Scale">
                <DetailRow label="Poids à vide" :value="formatNumber(tractor.emptyWeightKg, ' kg', 0)" />

                <DetailRow label="PTRA" :value="formatNumber(tractor.grossCombinationWeightKg, ' kg', 0)" />
            </DetailSection>

            <!-- Dimensions -->
            <DetailSection title="Dimensions" :icon="Ruler">
                <DetailRow label="Hauteur" :value="formatNumber(tractor.heightCm, ' cm', 0)" />

                <DetailRow label="Largeur" :value="formatNumber(tractor.widthCm, ' cm', 0)" />

                <DetailRow label="Longueur" :value="formatNumber(tractor.lengthCm, ' cm', 0)" />
            </DetailSection>

            <!-- Achat et amortissement -->
            <DetailSection title="Achat et amortissement" :icon="CircleDollarSign">
                <DetailRow label="Coût d’achat" :value="formatCurrency(tractor.purchaseCost)" />

                <DetailRow label="Début d’amortissement" :value="formatDate(tractor.depreciationStartDate)" />

                <DetailRow label="Fin d’amortissement" :value="formatDate(tractor.depreciationEndDate)" />
            </DetailSection>

            <!-- Synchronisation -->
            <DetailSection title="Synchronisation" :icon="Tag">
                <DetailRow label="Source" :value="tractor.externalSource ?? 'Non renseignée'" />

                <DetailRow label="Identifiant externe" :value="tractor.externalId
                    || 'Aucun identifiant externe'
                    " break-value />

                <DetailRow label="Créé le" :value="formatDateTime(tractor.createdAt)" />

                <DetailRow label="Modifié le" :value="formatDateTime(tractor.updatedAt)" />
            </DetailSection>
        </div>

        <!-- Actions -->
        <template v-if="tractor" #footer>
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

    <UpdateTractorModal :show="showUpdateModal" :tractor="tractor" @close="closeUpdateModal"
        @updated="handleTractorUpdated" />

    <DeleteTractorModal :show="showDeleteModal" :tractor="tractor" @close="closeDeleteModal"
        @deleted="handleTractorDeleted" />
</template>