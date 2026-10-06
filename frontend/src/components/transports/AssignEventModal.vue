<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import AppModal from '@/components/ui/AppModal.vue'

import { getCostParameters } from '@/api/cost/costParameterApi'
import { getPointsOfInterest } from '@/api/pointOfInterestApi'
import { createEvent } from '@/api/eventApi'
import { getApiErrorMessage } from '@/api/utils'
import { useNotification } from '@/composables/useNotification'
import { formatVehicleLabel } from '@/utils/vehicleUtils'

import type { CostParameterLight } from '@/models/cost/CostParameter'
import type { PointOfInterest } from '@/models/PointOfInterest'
import type { TransportDetail } from '@/models/transport/TransportDetail'

const props = defineProps<{
    show: boolean
    transport: TransportDetail | null
}>()

const emit = defineEmits<{
    close: []
    created: []
}>()

const notification = useNotification()

const costParameters = ref<CostParameterLight[]>([])
const pointsOfInterest = ref<PointOfInterest[]>([])

const costParameterId = ref<number | null>(null)
const pointOfInterestId = ref<number | null>(null)
const target = ref<'tractor' | 'semiTrailer'>('tractor')

const loading = ref(false)
const saving = ref(false)

const hasTractor = computed(() => props.transport?.tractorId != null)
const hasSemiTrailer = computed(() => props.transport?.semiTrailerId != null)

const canSubmit = computed(() =>
    costParameterId.value !== null
    && ((target.value === 'tractor' && hasTractor.value) || (target.value === 'semiTrailer' && hasSemiTrailer.value))
)

watch(() => props.show, async (visible) => {
    if (!visible) return

    costParameterId.value = null
    pointOfInterestId.value = null
    target.value = hasTractor.value || !hasSemiTrailer.value ? 'tractor' : 'semiTrailer'

    loading.value = true

    try {
        const [parameters, points] = await Promise.all([getCostParameters(), getPointsOfInterest()])

        costParameters.value = parameters.filter(parameter => parameter.assignmentType === 'MANUAL' && parameter.active)
        pointsOfInterest.value = points
    } catch (error) {
        notification.error('Chargement impossible', getApiErrorMessage(error, 'Les données nécessaires n’ont pas pu être chargées.'))
    } finally {
        loading.value = false
    }
}, { immediate: true })

const submit = async () => {
    if (!props.transport || !canSubmit.value || costParameterId.value === null) return

    try {
        saving.value = true

        await createEvent({
            serviceId: props.transport.id,
            costParameterId: costParameterId.value,
            pointOfInterestId: pointOfInterestId.value,
            tractorId: target.value === 'tractor' ? props.transport.tractorId : null,
            semiTrailerId: target.value === 'semiTrailer' ? props.transport.semiTrailerId : null
        })

        notification.success('Événement créé', 'L’événement a bien été assigné.')

        emit('created')
        emit('close')
    } catch (error) {
        notification.error('Création impossible', getApiErrorMessage(error, 'L’événement n’a pas pu être créé.'))
    } finally {
        saving.value = false
    }
}

const selectClass = 'w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-100'
</script>

<template>
    <AppModal :show="show" panel-class="max-w-lg" @close="!saving && emit('close')">
        <h2 class="mb-4 text-xl font-semibold text-slate-900">Assigner un événement</h2>

        <form class="space-y-4" @submit.prevent="submit">
            <div>
                <label class="mb-1 block text-sm font-medium text-slate-700">Paramètre de coût</label>
                <select v-model="costParameterId" :disabled="loading || saving" :class="selectClass" required>
                    <option :value="null" disabled>Sélectionner un paramètre</option>
                    <option v-for="parameter in costParameters" :key="parameter.id" :value="parameter.id">
                        {{ parameter.label }}
                    </option>
                </select>
                <p v-if="!loading && costParameters.length === 0" class="mt-1 text-xs text-slate-500">
                    Aucun paramètre d’assignation manuelle disponible.
                </p>
            </div>

            <div>
                <label class="mb-1 block text-sm font-medium text-slate-700">Point d’intérêt</label>
                <select v-model="pointOfInterestId" :disabled="loading || saving" :class="selectClass">
                    <option :value="null">Aucun</option>
                    <option v-for="point in pointsOfInterest" :key="point.id" :value="point.id">
                        {{ point.label }}
                    </option>
                </select>
            </div>

            <fieldset>
                <legend class="mb-1 text-sm font-medium text-slate-700">Véhicule concerné</legend>

                <div class="space-y-2">
                    <label class="flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-sm"
                        :class="hasTractor ? 'cursor-pointer' : 'cursor-not-allowed opacity-50'">
                        <input v-model="target" type="radio" value="tractor" :disabled="!hasTractor || saving" />
                        <span>
                            <span class="font-medium">Tracteur</span>
                            <span class="block text-xs text-slate-500">
                                {{ transport?.tractorRegistration ?? 'Aucun tracteur' }}
                                {{ formatVehicleLabel(transport?.tractorBrand ?? null, transport?.tractorModel ?? null) }}
                            </span>
                        </span>
                    </label>

                    <label class="flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-sm"
                        :class="hasSemiTrailer ? 'cursor-pointer' : 'cursor-not-allowed opacity-50'">
                        <input v-model="target" type="radio" value="semiTrailer" :disabled="!hasSemiTrailer || saving" />
                        <span>
                            <span class="font-medium">Semi-remorque</span>
                            <span class="block text-xs text-slate-500">
                                {{ transport?.semiTrailerRegistration ?? 'Aucune semi-remorque' }}
                                {{ formatVehicleLabel(transport?.semiTrailerBrand ?? null, transport?.semiTrailerModel ?? null) }}
                            </span>
                        </span>
                    </label>
                </div>
            </fieldset>

            <div class="flex justify-end gap-3 pt-2">
                <button type="button" :disabled="saving"
                    class="rounded-xl border border-slate-300 px-4 py-2 text-slate-700 transition hover:bg-slate-50 disabled:opacity-50"
                    @click="emit('close')">
                    Annuler
                </button>

                <button type="submit" :disabled="!canSubmit || saving"
                    class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                    {{ saving ? 'Création...' : 'Créer' }}
                </button>
            </div>
        </form>
    </AppModal>
</template>
