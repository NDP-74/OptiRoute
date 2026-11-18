<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Building2, Euro, RefreshCw } from 'lucide-vue-next'

import { getCustomers } from '@/api/customerApi'
import { updateServiceInfo } from '@/api/transportApi'
import { getApiErrorMessage } from '@/api/utils'
import { useNotification } from '@/composables/useNotification'

import type { CustomerSummary } from '@/models/Customer'
import type { TransportDetail } from '@/models/transport/TransportDetail'

const props = defineProps<{
    transport: TransportDetail
}>()

const emit = defineEmits<{
    cancel: []
    saved: []
    replan: []
}>()

const notification = useNotification()
const customers = ref<CustomerSummary[]>([])
const customerId = ref<number | null>(props.transport.customerId)
const revenue = ref<number | ''>(serviceRevenue(props.transport))
const loadingDurationMinutes = ref<number | ''>(props.transport.serviceTiming?.loadingDurationMinutes ?? '')
const unloadingDurationMinutes = ref<number | ''>(props.transport.serviceTiming?.unloadingDurationMinutes ?? '')
const isSaving = ref(false)

// Même règle de nommage qu'à la création : donneur d'ordre, sinon trajet à vide, sinon origine - destination
const serviceName = computed(() => {
    const customerName = customers.value.find(customer => customer.id === customerId.value)?.name?.trim()
    if (customerName) return customerName

    const routes = props.transport.routes.length ? props.transport.routes : [props.transport]
    if (routes.every(route => route.emptyTrip)) return 'Trajet à vide'
    return `${routes[0]?.originName?.trim() || 'Départ'} - ${routes[routes.length - 1]?.destinationName?.trim() || 'Arrivée'}`
})

function serviceRevenue(transport: TransportDetail): number | '' {
    if (!transport.routes.length) return transport.revenue ?? ''
    const revenues = transport.routes.map(route => route.revenue)
    return Math.round(revenues.reduce((total, value) => total + value, 0) * 100) / 100
}

onMounted(async () => {
    try {
        customers.value = await getCustomers()
    } catch (error) {
        notification.error('Chargement impossible', getApiErrorMessage(error, 'Les donneurs d’ordre n’ont pas pu être chargés.'))
    }
})

function cancel() {
    if (!isSaving.value) emit('cancel')
}

async function save() {
    if (isSaving.value) return
    if ([loadingDurationMinutes.value, unloadingDurationMinutes.value]
        .some(value => value !== '' && (!Number.isInteger(value) || value < 0))) {
        notification.error('Enregistrement impossible', 'Les durées doivent être des nombres entiers positifs ou nuls.')
        return
    }
    if (revenue.value !== '' && revenue.value < 0) {
        notification.error('Enregistrement impossible', 'Le chiffre d’affaires ne peut pas être négatif.')
        return
    }

    isSaving.value = true
    try {
        await updateServiceInfo(props.transport.id, {
            name: serviceName.value,
            customerId: customerId.value,
            revenue: revenue.value === '' ? null : revenue.value,
            loadingDurationMinutes: loadingDurationMinutes.value === '' ? null : loadingDurationMinutes.value,
            unloadingDurationMinutes: unloadingDurationMinutes.value === '' ? null : unloadingDurationMinutes.value,
        })
        notification.success('Service mis à jour', 'Les itinéraires et horaires sont inchangés.')
        emit('saved')
    } catch (error) {
        notification.error('Enregistrement impossible', getApiErrorMessage(error, 'Le service n’a pas pu être enregistré.'))
    } finally {
        isSaving.value = false
    }
}
</script>

<template>
    <div class="flex h-full min-h-0 flex-col">
        <div class="custom-scrollbar grid min-h-0 flex-1 content-start gap-5 overflow-auto lg:grid-cols-2">
            <section class="space-y-5 rounded-2xl border border-slate-200 bg-slate-50/60 p-5">
                <div>
                    <h3 class="text-sm font-semibold text-slate-800">Informations du service</h3>
                    <p class="text-xs text-slate-500">Ces informations ne modifient ni les itinéraires ni les horaires.</p>
                </div>

                <label class="block space-y-1.5">
                    <span class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                        <Building2 :size="14" /> Donneur d’ordre
                    </span>
                    <select v-model="customerId" class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm">
                        <option :value="null">Aucun donneur d’ordre</option>
                        <option v-for="customer in customers" :key="customer.id" :value="customer.id">{{ customer.name }}</option>
                    </select>
                </label>

                <label class="block space-y-1.5">
                    <span class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
                        <Euro :size="14" /> Chiffre d’affaires du service (€)
                    </span>
                    <input v-model.number="revenue" type="number" min="0" step="0.01" placeholder="0"
                        class="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm" />
                    <span v-if="transport.routes.length > 1" class="block text-xs text-slate-500">
                        Réparti entre les {{ transport.routes.length }} trajets au prorata des distances.
                    </span>
                </label>

                <fieldset class="grid grid-cols-2 gap-3">
                    <legend class="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Durées (minutes)</legend>
                    <label class="block text-xs text-slate-500">
                        Chargement
                        <input v-model.number="loadingDurationMinutes" type="number" min="0" step="1"
                            class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm" />
                    </label>
                    <label class="block text-xs text-slate-500">
                        Déchargement
                        <input v-model.number="unloadingDurationMinutes" type="number" min="0" step="1"
                            class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm" />
                    </label>
                </fieldset>
            </section>

            <section class="space-y-3 rounded-2xl border border-slate-200 p-5">
                <h3 class="text-sm font-semibold text-slate-800">Replanifier tout le service</h3>
                <p class="text-sm text-slate-500">
                    Recalcule l’ensemble des trajets à partir d’un nouvel horaire ou d’un nouvel itinéraire global.
                    Les modifications faites trajet par trajet seront remplacées, et un service issu de la simulation
                    Dashdoc repassera en mode manuel.
                </p>
                <button type="button" :disabled="isSaving"
                    class="inline-flex items-center gap-2 rounded-xl border border-blue-200 px-4 py-2 text-sm font-medium text-blue-700 transition hover:bg-blue-50 disabled:opacity-50"
                    @click="emit('replan')">
                    <RefreshCw class="h-4 w-4" />
                    Replanifier le service
                </button>
            </section>
        </div>

        <div class="flex shrink-0 justify-end gap-3 pt-4">
            <button type="button" class="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                :disabled="isSaving" @click="cancel">
                Annuler
            </button>
            <button type="button"
                class="rounded-xl bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:opacity-50"
                :disabled="isSaving" @click="save">
                {{ isSaving ? 'Enregistrement…' : 'Mettre à jour le service' }}
            </button>
        </div>
    </div>
</template>
