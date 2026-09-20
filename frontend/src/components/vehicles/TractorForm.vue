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

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Marque
                    </label>

                    <input v-model="form.brand" type="text" maxlength="100" placeholder="Volvo" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Modèle
                    </label>

                    <input v-model="form.model" type="text" maxlength="100" placeholder="FH16 540" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>
            </div>
        </section>

        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Motorisation
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Performances et consommation du véhicule.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Type de carburant
                    </label>

                    <select v-model="form.fuelType" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option value="DIESEL">Diesel</option>
                        <option value="B100">B100</option>
                        <option value="HVO100">HVO100</option>
                        <option value="ELECTRIC">Électrique</option>
                        <option value="LNG">GNL</option>
                        <option value="CNG">GNC</option>
                        <option value="HYBRID">Hybride</option>
                        <option value="OTHER">Autre</option>
                    </select>
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Vitesse maximale
                    </label>

                    <input v-model.number="form.maxSpeed" type="number" min="0" step="1" placeholder="90"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        km/h
                    </p>
                </div>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Consommation moyenne en charge
                    </label>

                    <input v-model.number="form.averageConsumption" type="number" min="0" step="0.01" placeholder="31.8"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Litres / 100 km
                    </p>
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Consommation moyenne à vide
                    </label>

                    <input v-model.number="form.averageConsumptionEmpty" type="number" min="0" step="0.01"
                        placeholder="28.5" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Litres / 100 km pour un trajet à vide
                    </p>
                </div>
            </div>
        </section>

        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Poids et dimensions
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Caractéristiques physiques utilisées pour les trajets.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Poids à vide
                    </label>

                    <input v-model.number="form.emptyWeightKg" type="number" min="0" step="1" placeholder="8200"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Kilogrammes
                    </p>
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        PTRA
                    </label>

                    <input v-model.number="form.grossCombinationWeightKg" type="number" min="0" step="1"
                        placeholder="44000" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Kilogrammes
                    </p>
                </div>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Hauteur
                    </label>

                    <input v-model.number="form.heightCm" type="number" min="0" step="1" placeholder="395"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Centimètres
                    </p>
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Largeur
                    </label>

                    <input v-model.number="form.widthCm" type="number" min="0" step="1" placeholder="255"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Centimètres
                    </p>
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Longueur
                    </label>

                    <input v-model.number="form.lengthCm" type="number" min="0" step="1" placeholder="610"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Centimètres
                    </p>
                </div>
            </div>

            <div>
                <label class="mb-1 block text-sm font-medium text-slate-700">
                    Nombre d’essieux
                </label>

                <input v-model.number="form.axleCount" type="number" min="1" step="1" placeholder="2"
                    :disabled="disabled"
                    class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
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
        </section>

    </div>
</template>