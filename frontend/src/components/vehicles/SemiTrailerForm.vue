<script setup lang="ts">
import type { SemiTrailerFormData } from "@/models/vehicle/SemiTrailer"

const form = defineModel<SemiTrailerFormData>({
    required: true,
})

withDefaults(
    defineProps<{ disabled?: boolean; trailerTypes?: string[] }>(),
    {
        disabled: false,
        trailerTypes: () => [],
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
                    Informations générales de la semi-remorque.
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

                    <input v-model="form.brand" type="text" maxlength="100" placeholder="Schmitz" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Modèle
                    </label>

                    <input v-model="form.model" type="text" maxlength="100" placeholder="S.CS Universal"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>
            </div>
        </section>

        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Caractéristiques
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Éléments techniques de la semi-remorque.
                </p>
            </div>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Type de semi-remorque
                    </label>

                    <select v-model="form.trailerType" :disabled="disabled || trailerTypes.length === 0"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option :value="null">
                            {{ trailerTypes.length === 0 ? 'Chargement des types...' : 'Sélectionner un type' }}
                        </option>

                        <option v-for="trailerType in trailerTypes" :key="trailerType" :value="trailerType">
                            {{ trailerType }}
                        </option>
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

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Nombre d’essieux
                    </label>

                    <input v-model.number="form.axleCount" type="number" min="1" step="1" placeholder="3"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>
            </div>
        </section>

        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Poids
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Limites de charge et masse à vide.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Poids à vide
                    </label>

                    <input v-model.number="form.emptyWeightKg" type="number" min="0" step="1" placeholder="6800"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Kilogrammes
                    </p>
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        PTAC
                    </label>

                    <input v-model.number="form.grossVehicleWeightKg" type="number" min="0" step="1" placeholder="34000"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Kilogrammes
                    </p>
                </div>
            </div>
        </section>

        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Dimensions
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Gabarit du véhicule pour la planification.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Hauteur
                    </label>

                    <input v-model.number="form.heightCm" type="number" min="0" step="1" placeholder="400"
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

                    <input v-model.number="form.lengthCm" type="number" min="0" step="1" placeholder="1360"
                        :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Centimètres
                    </p>
                </div>
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

                <input v-model.number="form.purchaseCost" type="number" min="0" step="0.01" placeholder="52000"
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