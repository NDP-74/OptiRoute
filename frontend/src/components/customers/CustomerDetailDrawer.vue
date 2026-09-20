<script setup lang="ts">
import { ref, watch } from "vue"
import { Building2, Tag, Edit3, MapPin, Trash2 } from "lucide-vue-next"

import { getCustomer } from "@/api/customerApi"
import DeleteCustomerModal from "@/components/customers/DeleteCustomerModal.vue"
import EditCustomerModal from "@/components/customers/EditCustomerModal.vue"
import AppDetailDrawer from "@/components/ui/AppDetailDrawer.vue"
import DetailRow from "@/components/ui/DetailRow.vue"
import DetailSection from "@/components/ui/DetailSection.vue"
import { formatDateTime } from "@/utils/formatters"

import type { CustomerDetails } from "@/models/Customer"

const props = defineProps<{
    open: boolean
    customerId: number | null
}>()

const emit = defineEmits<{
    close: []
    updated: []
    deleted: []
}>()

const customer = ref<CustomerDetails | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const showUpdateModal = ref(false)
const showDeleteModal = ref(false)
let currentRequestId = 0

const loadCustomer = async () => {
    if (!props.customerId) {
        customer.value = null
        return
    }

    const requestId = ++currentRequestId

    try {
        loading.value = true
        error.value = null
        customer.value = null
        const response = await getCustomer(props.customerId)

        if (requestId === currentRequestId) {
            customer.value = response
        }
    } catch {
        if (requestId === currentRequestId) {
            error.value = "Impossible de récupérer les informations du donneur d'ordre."
        }
    } finally {
        if (requestId === currentRequestId) {
            loading.value = false
        }
    }
}

const handleCustomerUpdated = async () => {
    showUpdateModal.value = false
    await loadCustomer()
    emit("updated")
}

const handleCustomerDeleted = () => {
    showDeleteModal.value = false
    customer.value = null
    emit("deleted")
    emit("close")
}

watch(
    () => [props.open, props.customerId],
    ([open, customerId]) => {
        if (open && customerId !== null) {
            loadCustomer()
            return
        }

        currentRequestId++
        customer.value = null
        error.value = null
        loading.value = false
        showUpdateModal.value = false
        showDeleteModal.value = false
    },
    { immediate: true },
)
</script>

<template>
    <AppDetailDrawer :open="props.open" :title="customer?.name" @close="emit('close')">
        <template #header>
            <div class="min-w-0">
                <p class="text-xs font-medium uppercase tracking-wide text-blue-600">
                    Détails du donneur d'ordre
                </p>
                <h2 class="mt-1 truncate text-xl font-bold text-slate-900">
                    {{ customer?.name ?? "Chargement..." }}
                </h2>
                <p v-if="customer?.code" class="mt-1 truncate text-sm text-slate-500">
                    {{ customer.code }}
                </p>
            </div>
        </template>

        <div v-if="loading" class="flex min-h-full items-center justify-center">
            <div class="text-center">
                <div class="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-slate-200 border-t-blue-600" />
                <p class="mt-3 text-sm text-slate-500">
                    Chargement des informations...
                </p>
            </div>
        </div>

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
                    @click="loadCustomer">
                    Réessayer
                </button>
            </div>
        </div>

        <div v-else-if="customer" class="space-y-6 p-6">
            <DetailSection title="Identification" :icon="Building2">
                <DetailRow label="Nom" :value="customer.name" />
                <DetailRow label="Code" :value="customer.code || 'Non renseigné'" />
            </DetailSection>

            <DetailSection title="Adresse" :icon="MapPin">
                <DetailRow label="Adresse" :value="customer.address || 'Non renseignée'" :break-value="true" />
                <DetailRow label="Ville" :value="customer.city || 'Non renseignée'" />
                <DetailRow label="Pays" :value="customer.country || 'Non renseigné'" />
            </DetailSection>

            <!-- Synchronisation -->
            <DetailSection title="Synchronisation" :icon="Tag">
                <DetailRow label="Source" :value="customer.externalSource || 'Non renseignée'" />
                <DetailRow label="Identifiant externe" :value="customer.externalId || 'Non renseigné'" />
                <DetailRow label="Créé le" :value="formatDateTime(customer.createdAt)" />
                <DetailRow label="Modifié le" :value="formatDateTime(customer.updatedAt)" />
            </DetailSection>
        </div>

        <template v-if="customer" #footer>
            <div class="flex items-center justify-between gap-3">
                <button type="button"
                    class="inline-flex items-center gap-2 rounded-xl border border-red-200 px-4 py-2 text-sm font-medium text-red-600 transition hover:bg-red-50"
                    @click="showDeleteModal = true">
                    <Trash2 class="h-4 w-4" />
                    Supprimer
                </button>
                <button type="button"
                    class="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700"
                    @click="showUpdateModal = true">
                    <Edit3 class="h-4 w-4" />
                    Modifier
                </button>
            </div>
        </template>
    </AppDetailDrawer>

    <EditCustomerModal :show="showUpdateModal" :customer="customer" @close="showUpdateModal = false"
        @updated="handleCustomerUpdated" />
    <DeleteCustomerModal :show="showDeleteModal" :customer="customer" @close="showDeleteModal = false"
        @deleted="handleCustomerDeleted" />
</template>