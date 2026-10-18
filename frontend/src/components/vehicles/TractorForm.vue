<script setup lang="ts">
import type { TractorFormData } from "@/models/vehicle/Tractor"

const form = defineModel<TractorFormData>({
    required: true,
})

withDefaults(
    defineProps<{ disabled?: boolean }>(),
    {
        disabled: false,
    },
)
</script>

<template>
    <div class="space-y-8">
        <section class="space-y-4">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Identification
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Informations générales du tracteur.
                </p>
            </div>

            <div>
                <label class="mb-1 block text-sm font-medium text-slate-700">
                    Immatriculation
                </label>

                <input v-model="form.registration" type="text" required maxlength="15" placeholder="AB-123-CD"
                    :disabled="disabled"
                    class="w-full rounded-xl border border-slate-300 px-4 py-3 uppercase outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
            </div>
        </section>

        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Achat et amortissement
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Données utilisées pour calculer les coûts fixes.
                </p>
            </div>

            <div>
                <label class="mb-1 block text-sm font-medium text-slate-700">
                    Coût d’achat
                </label>

                <input v-model.number="form.purchaseCost" type="number" min="0" step="0.01" placeholder="128500"
                    :disabled="disabled"
                    class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                <p class="mt-1 text-xs text-slate-500">
                    Euros
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Début d’amortissement
                    </label>

                    <input v-model="form.depreciationStartDate" type="date" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Fin d’amortissement
                    </label>

                    <input v-model="form.depreciationEndDate" type="date" :min="form.depreciationStartDate || undefined"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>
            </div>

            <div>
                <label class="mb-1 block text-sm font-medium text-slate-700">
                    Entretien
                </label>

                <input v-model.number="form.maintenanceCostPerKm" type="number" min="0" step="0.0001" placeholder="0.12"
                    :disabled="disabled"
                    class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                <p class="mt-1 text-xs text-slate-500">
                    Coût kilométrique d’entretien (€/km)
                </p>
            </div>
        </section>

    </div>
</template>