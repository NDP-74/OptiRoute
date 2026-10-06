<script setup lang="ts">
import { ref, watch, } from "vue"

import {
    CircleDollarSign,
    Edit3,
    Info,
    Ruler,
    Scale,
    Tag,
    Gauge,
    Trash2,
    Fuel,
} from "lucide-vue-next"

import type { SemiTrailerDetails } from "@/models/vehicle/SemiTrailer"

import { getSemiTrailer } from "@/api/vehicle/semiTrailerApi"
import { getApiErrorMessage } from "@/api/utils"

import DetailSection from "@/components/ui/DetailSection.vue"
import DetailRow from "@/components/ui/DetailRow.vue"
import AppDetailDrawer from "@/components/ui/AppDetailDrawer.vue"
import DeleteSemiTrailerModal from "@/components/vehicles/DeleteSemiTrailerModal.vue"
import UpdateSemiTrailerModal from "@/components/vehicles/UpdateSemiTrailerModal.vue"

import {
    formatCurrency, formatDate, formatDateTime, formatNumber,
} from "@/utils/formatters"
import { formatVehicleLabel } from "@/utils/vehicleUtils"

//Variables

const props = defineProps<{
    open: boolean
    semiTrailerId: number | null
}>()

const emit = defineEmits<{
    close: []
    updated: []
    deleted: []
}>()

const semiTrailer = ref<SemiTrailerDetails | null>(null)

const loading = ref(false)
const error = ref<string | null>(null)

/*
* Permet d’ignorer une ancienne réponse HTTP lorsque
* l’utilisateur sélectionne rapidement plusieurs tracteurs.
*/
let currentRequestId = 0

const loadSemiTrailer = async () => {
    if (props.semiTrailerId === null) {
        return
    }

    const requestId = ++currentRequestId

    try {
        loading.value = true
        error.value = null
        semiTrailer.value = null

        const response = await getSemiTrailer(props.semiTrailerId)

        if (requestId !== currentRequestId) {
            return
        }

        semiTrailer.value = response
    } catch (err: unknown) {
        if (requestId !== currentRequestId) {
            return
        }

        error.value = getApiErrorMessage(err, "Impossible de charger les informations de la semi-remorque.",)
    } finally {
        if (requestId === currentRequestId) {
            loading.value = false
        }
    }
}

//Manage Modal

const showUpdateModal = ref(false)
const showDeleteModal = ref(false)

const openUpdateModal = () => {
    if (!semiTrailer.value) {
        return
    }

    showUpdateModal.value = true
}

const closeUpdateModal = () => {
    showUpdateModal.value = false
}

const openDeleteModal = () => {
    if (!semiTrailer.value) {
        return
    }

    showDeleteModal.value = true
}

const closeDeleteModal = () => {
    showDeleteModal.value = false
}

//Handle

const handleSemiTrailerUpdated = async () => {
    showUpdateModal.value = false

    await loadSemiTrailer()
    emit("updated")
}

const handleSemiTrailerDeleted = () => {
    showDeleteModal.value = false
    semiTrailer.value = null

    emit("deleted")
    emit("close")
}

//Watch

watch(
    [() => props.open, () => props.semiTrailerId],
    ([open, semiTrailerId]) => {
        if (open && semiTrailerId !== null) {
            loadSemiTrailer()
            return
        }

        /* Invalide la requête en cours lors de la fermeture. */
        currentRequestId++

        semiTrailer.value = null
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
    <AppDetailDrawer :open="props.open" :title="semiTrailer?.registration ?? 'Détail de la semi-remorque'"
        @close="emit('close')">
        <template #header>
            <div class="min-w-0">
                <p class="text-xs font-medium uppercase tracking-wide text-blue-600">
                    Détails de la semi-remorque
                </p>

                <h2 class="mt-1 truncate text-xl font-bold text-slate-900">
                    {{ semiTrailer?.registration ?? "Chargement..." }}
                </h2>

                <p v-if="semiTrailer" class="mt-1 truncate text-sm text-slate-500">
                    {{ formatVehicleLabel(semiTrailer?.brand, semiTrailer?.model) }}
                </p>
            </div>
        </template>

        <!-- Chargement -->
        <div v-if="loading" class="flex min-h-full items-center justify-center">
            <div class="text-center">
                <LoaderCircle class="mx-auto h-8 w-8 animate-spin text-blue-600" />
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
                    @click="loadSemiTrailer">
                    Réessayer
                </button>
            </div>
        </div>

        <!-- Contenu -->
        <div v-else-if="semiTrailer" class="space-y-6 p-6">

            <!-- Identification -->
            <DetailSection title="Identification" :icon="Info">
                <DetailRow label="Immatriculation" :value="semiTrailer.registration" />

                <DetailRow label="Marque" :value="semiTrailer.brand ?? 'Non renseignée'" />

                <DetailRow label="Modèle" :value="semiTrailer.model ?? 'Non renseigné'" />

                <DetailRow label="Type" :value="semiTrailer.trailerType ?? 'Non renseigné'" />
            </DetailSection>

            <!-- Motorisation -->
            <DetailSection title="Motorisation" :icon="Fuel">
                <DetailRow label="Vitesse maximale" :value="formatNumber(semiTrailer.maxSpeed, ' km/h', 0)" />
            </DetailSection>

            <!-- Configuration -->
            <DetailSection title="Configuration" :icon="Gauge">
                <DetailRow label="Nombre d’essieux" :value="String(semiTrailer.axleCount)" />
            </DetailSection>

            <!-- Poids -->
            <DetailSection title="Poids" :icon="Scale">
                <DetailRow label="Poids à vide" :value="formatNumber(semiTrailer.emptyWeightKg, ' kg', 0)" />

                <DetailRow label="PTAC" :value="formatNumber(semiTrailer.grossVehicleWeightKg, ' kg', 0)" />
            </DetailSection>

            <!-- Dimensions -->
            <DetailSection title="Dimensions" :icon="Ruler">
                <DetailRow label="Hauteur" :value="formatNumber(semiTrailer.heightCm, ' cm', 0)" />

                <DetailRow label="Largeur" :value="formatNumber(semiTrailer.widthCm, ' cm', 0)" />

                <DetailRow label="Longueur" :value="formatNumber(semiTrailer.lengthCm, ' cm', 0)" />
            </DetailSection>

            <!-- Achat et amortissement -->
            <DetailSection title="Achat et amortissement" :icon="CircleDollarSign">
                <DetailRow label="Coût d’achat" :value="formatCurrency(semiTrailer.purchaseCost)" />

                <DetailRow label="Début d’amortissement" :value="formatDate(semiTrailer.depreciationStartDate)" />

                <DetailRow label="Fin d’amortissement" :value="formatDate(semiTrailer.depreciationEndDate)" />
            </DetailSection>

            <!-- Synchronisation -->
            <DetailSection title="Synchronisation" :icon="Tag">
                <DetailRow label="Source" :value="semiTrailer.externalSource ?? 'Non renseignée'" />

                <DetailRow label="Identifiant externe" :value="semiTrailer.externalId ?? 'Non renseigné'" />

                <DetailRow label="Créé le" :value="formatDateTime(semiTrailer.createdAt)" />

                <DetailRow label="Modifié le" :value="formatDateTime(semiTrailer.updatedAt)" />
            </DetailSection>
        </div>

        <template v-if="semiTrailer" #footer>
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

    <UpdateSemiTrailerModal :show="showUpdateModal" :semi-trailer="semiTrailer" @close="closeUpdateModal"
        @updated="handleSemiTrailerUpdated" />

    <DeleteSemiTrailerModal :show="showDeleteModal" :semi-trailer="semiTrailer" @close="closeDeleteModal"
        @deleted="handleSemiTrailerDeleted" />
</template>