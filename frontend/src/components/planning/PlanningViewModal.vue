<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowDown, ArrowUp, Plus, X } from 'lucide-vue-next'
import AppModal from '@/components/ui/AppModal.vue'
import { getDrivers } from '@/api/driver/driverApi'
import { deletePlanningView, savePlanningView } from '@/api/planningApi'
import { getApiErrorMessage } from '@/api/utils'
import { useNotification } from '@/composables/useNotification'
import type { PlanningViewRow, SavedPlanningView } from '@/models/planning/PlanningView'

const props = defineProps<{ view: SavedPlanningView | null }>()
const emit = defineEmits<{ close: []; saved: [SavedPlanningView]; deleted: [number] }>()
const notification = useNotification()
const name = ref(props.view?.name ?? '')
const rows = ref<PlanningViewRow[]>([...(props.view?.rows ?? [])])
const drivers = ref(new Map<number, string>())
const search = ref('')
const loading = ref(true)
const busy = ref(false)
const confirmingDelete = ref(false)
const unusedRow: PlanningViewRow = { type: 'UNUSED_VEHICLES', driverId: null }

function rowLabel(row: PlanningViewRow) {
    return row.type === 'UNUSED_VEHICLES'
        ? 'Véhicules non utilisés'
        : drivers.value.get(row.driverId) ?? (loading.value ? `Chauffeur ${row.driverId}` : `Chauffeur indisponible (${row.driverId})`)
}

const available = computed(() => {
    const selected = new Set(rows.value.map(row => row.driverId))
    const term = search.value.trim().toLocaleLowerCase('fr')
    const driverRows: PlanningViewRow[] = [...drivers.value.keys()].map(driverId => ({ type: 'DRIVER', driverId }))
    return [unusedRow, ...driverRows].filter(row =>
        !selected.has(row.driverId) && rowLabel(row).toLocaleLowerCase('fr').includes(term))
})

async function loadDrivers() {
    loading.value = true
    try {
        const entries = (await getDrivers()).map(driver => [driver.id, `${driver.firstName} ${driver.lastName}`] as const)
        drivers.value = new Map(entries.sort((a, b) => a[1].localeCompare(b[1], 'fr')))
    } catch (error) {
        notification.error('Chargement impossible', getApiErrorMessage(error, 'Le catalogue des chauffeurs n’a pas pu être chargé.'))
    } finally {
        loading.value = false
    }
}
onMounted(loadDrivers)

function move(index: number, step: number) {
    const other = index + step
    const first = rows.value[index]
    const second = rows.value[other]
    if (!first || !second) return
    rows.value[index] = second
    rows.value[other] = first
}

async function save() {
    if (busy.value || confirmingDelete.value || !name.value.trim() || !rows.value.length) return
    busy.value = true
    try {
        emit('saved', await savePlanningView({ name: name.value.trim(), rows: rows.value }, props.view?.id))
    } catch (error) {
        notification.error('Enregistrement impossible', getApiErrorMessage(error, 'La vue n’a pas pu être enregistrée.'))
    } finally {
        busy.value = false
    }
}

async function remove() {
    const id = props.view?.id
    if (id === undefined || busy.value) return
    busy.value = true
    try {
        await deletePlanningView(id)
        emit('deleted', id)
    } catch (error) {
        notification.error('Suppression impossible', getApiErrorMessage(error, 'La vue n’a pas pu être supprimée.'))
    } finally {
        busy.value = false
    }
}
</script>

<template>
    <AppModal show panel-class="max-h-[calc(100dvh-2rem)] max-w-3xl overflow-y-auto" @close="!busy && emit('close')">
        <form class="space-y-4" @submit.prevent="save">
            <h2 class="text-xl font-semibold text-slate-900">{{ view ? 'Modifier la vue' : 'Créer une vue' }}</h2>
            <p class="text-sm text-slate-600">Choisissez les lignes à afficher et leur ordre. Cette vue est personnelle.</p>
            <fieldset :disabled="busy || confirmingDelete" class="space-y-4 disabled:opacity-60">
                <label class="block text-sm font-medium text-slate-700">
                    Nom de la vue
                    <input v-model="name" required maxlength="100" class="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2" />
                </label>
                <div class="grid gap-4 sm:grid-cols-2">
                    <section class="min-w-0 space-y-2">
                        <h3 class="text-sm font-semibold">Lignes disponibles</h3>
                        <input v-model="search" type="search" aria-label="Rechercher une ligne" placeholder="Rechercher…"
                            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
                        <ul class="h-56 overflow-y-auto rounded-lg border border-slate-200">
                            <li v-if="loading" class="p-3 text-sm text-slate-500">
                                <span role="status">Chargement des chauffeurs…</span>
                            </li>
                            <li v-for="row in available" :key="row.driverId ?? row.type">
                                <button type="button" class="flex w-full items-center justify-between gap-2 px-3 py-2 text-left text-sm hover:bg-blue-50"
                                    @click="rows.push(row)">
                                    {{ rowLabel(row) }} <Plus :size="16" aria-hidden="true" />
                                </button>
                            </li>
                            <li v-if="!available.length && !loading" class="p-3 text-sm text-slate-500">Aucune ligne disponible.</li>
                        </ul>
                    </section>
                    <section class="min-w-0 space-y-2">
                        <h3 class="text-sm font-semibold">Ordre d’affichage</h3>
                        <ol class="h-56 divide-y divide-slate-100 overflow-y-auto rounded-lg border border-slate-200">
                            <li v-for="(row, index) in rows" :key="row.driverId ?? row.type" class="flex items-center justify-between gap-2 px-3 py-2 text-sm">
                                <span>{{ index + 1 }}. {{ rowLabel(row) }}</span>
                                <div class="flex shrink-0 gap-1">
                                    <button type="button" :disabled="index === 0" :aria-label="`Monter ${rowLabel(row)}`"
                                        class="rounded p-1 hover:bg-slate-100 disabled:opacity-30" @click="move(index, -1)"><ArrowUp :size="16" /></button>
                                    <button type="button" :disabled="index === rows.length - 1" :aria-label="`Descendre ${rowLabel(row)}`"
                                        class="rounded p-1 hover:bg-slate-100 disabled:opacity-30" @click="move(index, 1)"><ArrowDown :size="16" /></button>
                                    <button type="button" :aria-label="`Retirer ${rowLabel(row)}`"
                                        class="rounded p-1 text-red-600 hover:bg-red-50" @click="rows.splice(index, 1)"><X :size="16" /></button>
                                </div>
                            </li>
                            <li v-if="!rows.length" class="p-3 text-sm text-slate-500">Ajoutez au moins une ligne.</li>
                        </ol>
                    </section>
                </div>
            </fieldset>
            <div v-if="confirmingDelete" class="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-red-50 p-3 text-sm">
                <p>Supprimer cette vue ? Les trajets ne seront pas modifiés.</p>
                <div class="flex gap-3">
                    <button type="button" :disabled="busy" @click="confirmingDelete = false">Annuler</button>
                    <button type="button" :disabled="busy" class="font-semibold text-red-700" @click="remove">Confirmer</button>
                </div>
            </div>
            <div v-else class="flex flex-wrap justify-end gap-3">
                <button v-if="view" type="button" :disabled="busy" class="mr-auto px-3 py-2 text-sm text-red-700"
                    @click="confirmingDelete = true">Supprimer la vue</button>
                <button type="button" :disabled="busy" class="rounded-lg border border-slate-300 px-3 py-2 text-sm"
                    @click="emit('close')">Annuler</button>
                <button type="submit" :disabled="busy || !name.trim() || !rows.length"
                    class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50">
                    {{ busy ? 'Enregistrement…' : 'Enregistrer la vue' }}
                </button>
            </div>
        </form>
    </AppModal>
</template>
