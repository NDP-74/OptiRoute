<script setup lang="ts">

import { computed, onMounted, ref } from "vue"

import type { CostConditionRequest, CreateCostParameterRequest } from "@/models/cost/CostParameter"
import { getSemiTrailerTypes } from "@/api/vehicle/semiTrailerApi"
import { assignmentTypeLabels, categoryOptions, conditionValueUnits, getFieldOptions, getOperatorOptions, sourceOptions, unitOptions } from "@/utils/costParameterUtils"

const form = defineModel<CreateCostParameterRequest>({
    required: true,
})

const hasConditions = computed(() =>
    form.value.assignmentType === "ALL_CONDITIONS" || form.value.assignmentType === "ANY_CONDITION",
)

withDefaults(
    defineProps<{
        disabled?: boolean
    }>(),
    {
        disabled: false,
    },
)

const vehicleTypeSelectOptions = ref<{ value: string; label: string }[]>([])

const loadVehicleTypeSelectOptions = async () => {
    try {
        const trailerTypeOptions = await getSemiTrailerTypes()
        vehicleTypeSelectOptions.value = trailerTypeOptions.map((option) => ({
            value: String(option.id),
            label: option.label,
        }))
    } catch {
        vehicleTypeSelectOptions.value = []
    }
}

onMounted(() => {
    loadVehicleTypeSelectOptions()
    normalizeExistingConditions()
})

// Conditions

const createEmptyCondition = (): CostConditionRequest => ({
    source: "TRIP",
    field: "DISTANCE",
    operator: "GREATER_THAN",
    value: "",
})

const addCondition = () => {
    form.value.conditions.push(
        createEmptyCondition(),
    )
}

const removeCondition = (index: number) => {
    form.value.conditions.splice(index, 1)
}

const handleAssignmentTypeChange = () => {
    if (!hasConditions.value) {
        form.value.conditions = []
    }
}

const handleSourceChange = (
    condition: CostConditionRequest,
) => {
    const fields = getFieldOptions(condition.source)

    condition.field = fields[0]?.value ?? "DISTANCE"
    condition.operator = getOperatorOptions(condition.field)[0]?.value ?? "EQUALS"
    condition.value = ""
}

const handleFieldChange = (
    condition: CostConditionRequest,
) => {
    condition.operator = getOperatorOptions(condition.field)[0]?.value ?? "EQUALS"
    condition.value = ""
}

const normalizeExistingConditions = () => {
    form.value.conditions.forEach((condition) => {
        if (!getOperatorOptions(condition.field).some((option) => option.value === condition.operator)) {
            condition.operator = getOperatorOptions(condition.field)[0]?.value ?? "EQUALS"
        }
    })
}

</script>

<template>
    <div class="space-y-8">

        <!-- Informations générales -->

        <section class="space-y-4">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Informations générales
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Identification et catégorie du coût référentiel.
                </p>
            </div>

            <div>
                <label class="mb-1.5 block text-sm font-medium text-slate-700">
                    Catégorie
                </label>

                <select v-model="form.category" required :disabled="disabled"
                    class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100">
                    <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
                        {{ option.label }}
                    </option>
                </select>
            </div>

            <div>
                <label class="mb-1.5 block text-sm font-medium text-slate-700">
                    Libellé
                </label>

                <input v-model="form.label" type="text" required maxlength="150" :disabled="disabled"
                    placeholder="Ex. Entretien"
                    class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
            </div>
        </section>

        <!-- Coût -->

        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Coût
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Montant et unité utilisés pour calculer le coût.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Valeur
                    </label>

                    <input v-model.number="form.value" type="number" min="0" step="0.01" required :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>

                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Unité
                    </label>

                    <select v-model="form.unit" required :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option v-for="option in unitOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                        </option>
                    </select>
                </div>
            </div>
        </section>

        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Application
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Logique d'application et statut du coût.
                </p>
            </div>

            <button type="button" role="switch" :aria-checked="form.active" aria-label="Activer le coût"
                :disabled="disabled"
                class="flex w-full items-center justify-between gap-4 rounded-xl border px-4 py-3 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                :class="form.active ? 'border-blue-200 bg-blue-50 hover:bg-blue-100' : 'border-slate-200 bg-slate-50 hover:bg-slate-100'"
                @click="form.active = !form.active">
                <span class="text-sm font-medium text-slate-700">Statut du coût</span>

                <span class="flex items-center gap-3">
                    <span class="text-sm font-semibold" :class="form.active ? 'text-blue-700' : 'text-slate-500'">
                        {{ form.active ? 'Actif' : 'Désactivé' }}
                    </span>
                    <span aria-hidden="true"
                        class="inline-flex h-6 w-11 shrink-0 items-center rounded-full p-0.5 transition-colors"
                        :class="form.active ? 'bg-blue-600' : 'bg-slate-300'">
                        <span
                            class="h-5 w-5 rounded-full bg-white shadow-sm transition-transform motion-reduce:transition-none"
                            :class="form.active ? 'translate-x-5' : 'translate-x-0'"></span>
                    </span>
                </span>
            </button>

            <div>
                <label class="mb-1.5 block text-sm font-medium text-slate-700">
                    Logique d'application
                </label>

                <select v-model="form.assignmentType" required :disabled="disabled" @change="handleAssignmentTypeChange"
                    class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100">
                    <option v-for="(label, value) in assignmentTypeLabels" :key="value" :value="value">
                        {{ label }}
                    </option>
                </select>
            </div>
        </section>

        <!-- Conditions -->

        <section v-if="hasConditions" class="space-y-4">
            <div class="flex items-center justify-between">
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Conditions
                </h3>

                <button type="button" :disabled="disabled"
                    class="text-sm font-medium text-blue-600 transition hover:text-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                    @click="addCondition">
                    + Ajouter une condition
                </button>
            </div>

            <!-- Pas de condition -->

            <div v-if="form.conditions.length === 0"
                class="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-500">
                Aucune condition.
                Ce coût sera toujours applicable.
            </div>

            <div v-for="(condition, index) in form.conditions" :key="index"
                class="rounded-xl border border-slate-200 bg-slate-50 p-4">
                <div class="mb-3 flex items-center justify-between">
                    <span class="text-sm font-semibold text-slate-700">
                        Condition {{ index + 1 }}
                    </span>

                    <button type="button" :disabled="disabled"
                        class="text-sm font-medium text-red-600 hover:text-red-700 disabled:opacity-50"
                        @click="removeCondition(index)">
                        Supprimer
                    </button>
                </div>

                <div class="space-y-3">

                    <!-- Source -->

                    <div>
                        <label class="mb-1 block text-xs font-medium text-slate-600">
                            Élément
                        </label>

                        <select v-model="condition.source" :disabled="disabled"
                            class="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-blue-500"
                            @change="handleSourceChange(condition)">
                            <option v-for="option in sourceOptions" :key="option.value" :value="option.value">
                                {{ option.label }}
                            </option>
                        </select>
                    </div>

                    <!-- Field -->

                    <div>
                        <label class="mb-1 block text-xs font-medium text-slate-600">
                            Paramètre
                        </label>

                        <select v-model="condition.field" :disabled="disabled"
                            class="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-blue-500"
                            @change="handleFieldChange(condition)">
                            <option v-for="option in getFieldOptions(condition.source)" :key="option.value"
                                :value="option.value">
                                {{ option.label }}
                            </option>
                        </select>
                    </div>

                    <!-- Operator -->

                    <div>
                        <label class="mb-1 block text-xs font-medium text-slate-600">
                            Condition
                        </label>

                        <select v-model="condition.operator" :disabled="disabled"
                            class="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-blue-500">
                            <option v-for="option in getOperatorOptions(condition.field)" :key="option.value"
                                :value="option.value">
                                {{ option.label }}
                            </option>
                        </select>
                    </div>

                    <!-- Value -->

                    <div>
                        <label class="mb-1 block text-xs font-medium text-slate-600">
                            Valeur{{ conditionValueUnits[condition.field] ? `
                            (${conditionValueUnits[condition.field]})` : "" }}
                        </label>

                        <!-- Heure -->

                        <input
                            v-if="['DEPARTURE_TIME', 'ARRIVAL_TIME', 'DRIVER_DAY_START_TIME', 'DRIVER_DAY_END_TIME'].includes(condition.field)"
                            v-model="condition.value" type="time" required :disabled="disabled"
                            class="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-blue-500" />

                        <!-- Booléen -->

                        <select v-else-if="['EMPTY_TRIP', 'LOADED_TRIP'].includes(condition.field)"
                            v-model="condition.value" required :disabled="disabled"
                            class="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-blue-500">
                            <option value="" disabled>
                                Sélectionner un état
                            </option>

                            <option value="true">
                                Oui
                            </option>

                            <option value="false">
                                Non
                            </option>
                        </select>

                        <!-- Type véhicule -->

                        <select v-else-if="condition.field === 'VEHICLE_TYPE'" v-model="condition.value" required
                            :disabled="disabled"
                            class="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-blue-500">
                            <option value="" disabled>
                                Sélectionner un type
                            </option>

                            <option v-for="option in vehicleTypeSelectOptions" :key="option.value"
                                :value="option.value">
                                {{ option.label }}
                            </option>
                        </select>

                        <!-- Valeur numérique -->

                        <input v-else v-model="condition.value" type="number" step="1" required :disabled="disabled"
                            class="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-blue-500" />
                    </div>
                </div>
            </div>

            <button type="button" :disabled="disabled"
                class="w-full rounded-xl border border-dashed border-blue-300 px-4 py-3 text-sm font-medium text-blue-600 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50"
                @click="addCondition">
                + Ajouter une condition
            </button>
        </section>
    </div>
</template>