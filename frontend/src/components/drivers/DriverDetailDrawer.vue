<script setup lang="ts">
import { ref, watch } from "vue"
import {
    Clock3,
    Edit3,
    Info,
    Phone,
    Trash2,
    WalletCards,
} from "lucide-vue-next"

import AppDetailDrawer from "@/components/ui/AppDetailDrawer.vue"
import DetailSection from "@/components/ui/DetailSection.vue"
import DetailRow from "@/components/ui/DetailRow.vue"
import UpdateDriverModal from "@/components/drivers/UpdateDriverModal.vue"
import DeleteDriverModal from "@/components/drivers/DeleteDriverModal.vue"

import { getDriver } from "@/api/driver/driverApi"

import { formatCurrency, formatNumber } from "@/utils/formatters"
import { getDriverName, getHourlyCost } from "@/utils/driverUtils"

import type { DriverDetails } from "@/models/driver/Driver"

//Variables

const props = defineProps<{
    open: boolean
    driverId: number | null
}>()

const emit = defineEmits<{
    close: []
    updated: []
    deleted: []
}>()

const loading = ref(false)
const error = ref<string | null>(null)

const driver = ref<DriverDetails | null>(null)
let currentRequestId = 0

const loadDriver = async () => {
    if (!props.driverId) {
        driver.value = null
        return
    }

    const requestId = ++currentRequestId

    try {
        loading.value = true
        error.value = null
        driver.value = null

        const response = await getDriver(props.driverId)

        if (requestId !== currentRequestId) {
            return
        }

        driver.value = response
    } catch (exception) {
        if (requestId !== currentRequestId) {
            return
        }

        error.value = "Impossible de récupérer les informations du chauffeur."
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

const handleDriverUpdated = async () => {
    closeUpdateModal()

    await loadDriver()

    emit("updated")
}

const handleDriverDeleted = () => {
    closeDeleteModal()

    driver.value = null

    emit("deleted")
    emit("close")
}

// Watch

watch(
    () => [props.open, props.driverId],
    ([open, driverId]) => {
        if (open && driverId !== null) {
            loadDriver()
            return
        }

        /* Invalide la requête en cours lors de la fermeture. */
        currentRequestId++

        driver.value = null
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
    <AppDetailDrawer :open="props.open" :title="getDriverName(driver)" @close="emit('close')">
        <template #header>
            <div class="min-w-0">
                <p class="text-xs font-medium uppercase tracking-wide text-blue-600">
                    Détails du chauffeur
                </p>

                <h2 class="mt-1 truncate text-xl font-bold text-slate-900">
                    {{ driver ? getDriverName(driver) : "Chargement..." }}
                </h2>

                <p v-if="driver?.login" class="mt-1 truncate text-sm text-slate-500">
                    {{ driver.login }}
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
                    @click="loadDriver">
                    Réessayer
                </button>
            </div>
        </div>

        <!-- Contenu -->
        <div v-else-if="driver" class="space-y-6 p-6">
            <DetailSection title="Identification" :icon="Info">
                <DetailRow label="Prénom" :value="driver.firstName || 'Non renseigné'" />

                <DetailRow label="Nom" :value="driver.lastName || 'Non renseigné'" />

                <DetailRow label="Login" :value="driver.login || 'Non renseigné'" />
            </DetailSection>

            <DetailSection title="Coordonnées" :icon="Phone">
                <DetailRow label="Téléphone" :value="driver.phoneNumber || 'Non renseigné'" />

                <DetailRow label="Lieu de stationnement" :value="driver.locationLabel || 'Non renseigné'"
                    :break-value="true" />
            </DetailSection>

            <DetailSection title="Véhicules affectés" :icon="Info">
                <DetailRow label="Tracteur" :value="driver.tractorRegistration || 'Aucun'" />

                <DetailRow label="Semi-remorque" :value="driver.semiTrailerRegistration || 'Aucune'" />
            </DetailSection>

            <DetailSection title="Profil du chauffeur" :icon="Info">
                <DetailRow label="Type de chauffeur" :value="driver.driverType === 'LONG_HAUL'
                    ? 'Grand routier'
                    : driver.driverType === 'SHORT_DISTANCE'
                        ? 'Courte distance'
                        : driver.driverType === 'REGIONAL' ? 'Régional' : 'Non renseigné'" />
            </DetailSection>

            <DetailSection title="Coût du chauffeur" :icon="WalletCards">
                <DetailRow label="Coût annuel" :value="driver.annualSalary != null
                    ? formatCurrency(driver.annualSalary)
                    : 'Non renseigné'" />

                <DetailRow label="Coût horaire estimé" :value="getHourlyCost(driver) != -1
                    ? `${formatCurrency(getHourlyCost(driver)!)} / h`
                    : 'Non calculable'" />

                <DetailRow label="Type de coût"
                    :value="driver.costType === 'HOURLY' ? 'Horaire' : driver.costType === 'FIXED' ? 'Fixe' : 'Non renseigné'" />
            </DetailSection>

            <DetailSection title="Temps de travail" :icon="Clock3">
                <DetailRow label="Heures mensuelles" :value="driver.monthlyWorkingHours != null
                    ? `${formatNumber(driver.monthlyWorkingHours)} h`
                    : 'Non renseignées'" />
            </DetailSection>
        </div>

        <!-- Actions -->
        <template v-if="driver" #footer>
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

    <UpdateDriverModal :show="showUpdateModal" :driver="driver" @close="closeUpdateModal"
        @updated="handleDriverUpdated" />

    <DeleteDriverModal :show="showDeleteModal" :driver="driver" @close="closeDeleteModal"
        @deleted="handleDriverDeleted" />
</template>