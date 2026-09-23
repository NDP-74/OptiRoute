<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { getTractors } from "@/api/vehicle/tractorApi"
import { getSemiTrailers } from "@/api/vehicle/semiTrailerApi"
import type { TractorSummary } from "@/models/vehicle/Tractor"
import type { SemiTrailerSummary } from "@/models/vehicle/SemiTrailer"
import type { DriverFormData } from "@/models/driver/Driver"
import HereAutocompleteInput from "@/components/maps/HereAutocompleteInput.vue"

const form = defineModel<DriverFormData>({
    required: true,
})

const tractors = ref<TractorSummary[]>([])
const semiTrailers = ref<SemiTrailerSummary[]>([])
const optionsError = ref(false)

const parkingLocation = computed({
    get: () => form.value.locationLabel ? { address: form.value.locationLabel } : undefined,
    set: (value: { address?: string; position?: { lat: number; lng: number } } | undefined) => {
        form.value.locationLabel = value?.address ?? null
        form.value.locationLatitude = value?.position?.lat ?? null
        form.value.locationLongitude = value?.position?.lng ?? null
    },
})

withDefaults(
    defineProps<{ disabled?: boolean }>(),
    {
        disabled: false,
    },
)

onMounted(async () => {
    try {
        const [tractorOptions, semiTrailerOptions] = await Promise.all([getTractors(), getSemiTrailers()])
        tractors.value = tractorOptions
        semiTrailers.value = semiTrailerOptions
    } catch {
        optionsError.value = true
    }
})
</script>

```vue
<template>
    <div class="space-y-8">

        <!-- IDENTIFICATION -->
        <section class="space-y-4">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Identification
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Informations générales du conducteur.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <!-- Prénom -->
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Prénom
                    </label>

                    <input v-model="form.firstName" type="text" required maxlength="100" :disabled="disabled" class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm
                               outline-none transition
                               placeholder:text-slate-400
                               focus:border-blue-500 focus:ring-2 focus:ring-blue-100
                               disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>

                <!-- Nom -->
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Nom
                    </label>

                    <input v-model="form.lastName" type="text" required maxlength="100" :disabled="disabled" class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm
                               outline-none transition
                               placeholder:text-slate-400
                               focus:border-blue-500 focus:ring-2 focus:ring-blue-100
                               disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>
            </div>

            <!-- Téléphone -->
            <div>
                <label class="mb-1.5 block text-sm font-medium text-slate-700">
                    Numéro de téléphone
                </label>

                <input v-model="form.phoneNumber" type="tel" maxlength="30" :disabled="disabled" class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm
                           outline-none transition
                           placeholder:text-slate-400
                           focus:border-blue-500 focus:ring-2 focus:ring-blue-100
                           disabled:cursor-not-allowed disabled:bg-slate-100" />
            </div>

            <!-- Lieu de stationnement -->
            <div>
                <label class="mb-1.5 block text-sm font-medium text-slate-700">
                    Lieu de stationnement
                </label>
                <HereAutocompleteInput v-model="parkingLocation" label="Lieu de stationnement" :disabled="disabled" />
            </div>
        </section>


        <!-- AFFECTATION -->
        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Affectation
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Véhicules habituellement associés au conducteur.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">

                <!-- Tracteur -->
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Tracteur
                    </label>

                    <select v-model="form.tractorId" :disabled="disabled" class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm
                               outline-none transition
                               focus:border-blue-500 focus:ring-2 focus:ring-blue-100
                               disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option :value="null">
                            Aucun tracteur
                        </option>

                        <option v-for="tractor in tractors" :key="tractor.id" :value="tractor.id">
                            {{ tractor.registration }}
                        </option>
                    </select>
                </div>

                <!-- Semi-remorque -->
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Semi-remorque
                    </label>

                    <select v-model="form.semiTrailerId" :disabled="disabled" class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm
                               outline-none transition
                               focus:border-blue-500 focus:ring-2 focus:ring-blue-100
                               disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option :value="null">
                            Aucune semi-remorque
                        </option>

                        <option v-for="semiTrailer in semiTrailers" :key="semiTrailer.id" :value="semiTrailer.id">
                            {{ semiTrailer.registration }}
                        </option>
                    </select>
                </div>
            </div>

            <p v-if="optionsError" class="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
                Impossible de charger les véhicules.
            </p>
        </section>


        <!-- RÉMUNÉRATION ET TEMPS DE TRAVAIL -->
        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Rémunération et temps de travail
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Informations utilisées pour calculer le coût du conducteur.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">

                <!-- Salaire annuel -->
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Salaire annuel
                    </label>

                    <div class="relative">
                        <input v-model.number="form.annualSalary" type="number" min="0" step="100" :disabled="disabled"
                            class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 pr-16 text-sm
                                   outline-none transition
                                   focus:border-blue-500 focus:ring-2 focus:ring-blue-100
                                   disabled:cursor-not-allowed disabled:bg-slate-100" />

                        <span class="pointer-events-none absolute inset-y-0 right-4 flex items-center
                                   text-sm text-slate-400">
                            €/an
                        </span>
                    </div>
                </div>

                <!-- Temps de travail -->
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Temps de travail mensuel
                    </label>

                    <div class="relative">
                        <input v-model.number="form.monthlyWorkingHours" type="number" min="0" step="1"
                            :disabled="disabled" class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 pr-20 text-sm
                                   outline-none transition
                                   focus:border-blue-500 focus:ring-2 focus:ring-blue-100
                                   disabled:cursor-not-allowed disabled:bg-slate-100" />

                        <span class="pointer-events-none absolute inset-y-0 right-4 flex items-center
                                   text-sm text-slate-400">
                            h/mois
                        </span>
                    </div>
                </div>
            </div>
        </section>


        <!-- PROFIL -->
        <section class="space-y-5 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Profil du chauffeur
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Paramètres utilisés pour déterminer le coût du conducteur.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">

                <!-- TYPE DE COÛT -->
                <div class="sm:col-span-2">
                    <div class="mb-2 flex items-center gap-2">
                        <label class="text-sm font-medium text-slate-700">
                            Type de coût
                        </label>

                        <!-- Info -->
                        <div class="group relative">
                            <button type="button" class="flex h-5 w-5 items-center justify-center rounded-full
                                       text-xs font-semibold text-slate-400 transition
                                       hover:bg-slate-100 hover:text-slate-600
                                       focus:outline-none focus:ring-2 focus:ring-blue-200"
                                aria-label="Informations sur les types de coût">
                                ?
                            </button>

                            <div
                                class="pointer-events-none absolute bottom-full left-0 z-20 mb-2 hidden w-80 rounded-xl border border-slate-200 bg-white p-4 text-left text-xs leading-relaxed text-slate-600 shadow-lg group-hover:block group-focus-within:block">
                                <div class="space-y-3">
                                    <div>
                                        <p class="font-semibold text-slate-800">
                                            Coût horaire
                                        </p>
                                        <p class="mt-1">
                                            Plus juste lorsque le conducteur dépasse régulièrement
                                            le nombre d'heures prévu dans son forfait et peut être
                                            amené à récupérer des repos compensateurs.
                                        </p>
                                    </div>

                                    <div class="border-t border-slate-100 pt-3">
                                        <p class="font-semibold text-slate-800">
                                            Coût forfaitaire
                                        </p>
                                        <p class="mt-1">
                                            Prend mieux en compte le salaire versé aux conducteurs
                                            effectuant moins d'heures que celles prévues dans leur forfait.
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Cartes de sélection -->
                    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">

                        <!-- Horaire -->
                        <label class="group relative flex cursor-pointer rounded-xl border p-4 transition" :class="form.costType === 'HOURLY'
                            ? 'border-blue-500 bg-blue-50/50 ring-1 ring-blue-500'
                            : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'">
                            <input v-model="form.costType" type="radio" value="HOURLY" :disabled="disabled"
                                class="sr-only" />

                            <div class="flex w-full items-start gap-3">
                                <div class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center
                                           rounded-full border transition" :class="form.costType === 'HOURLY'
                                            ? 'border-blue-500 bg-blue-500'
                                            : 'border-slate-300 group-hover:border-slate-400'">
                                    <div v-if="form.costType === 'HOURLY'" class="h-2 w-2 rounded-full bg-white" />
                                </div>

                                <div>
                                    <p class="text-sm font-semibold text-slate-800">
                                        Horaire
                                    </p>

                                    <p class="mt-1 text-xs leading-relaxed text-slate-500">
                                        Calculé en fonction du temps réellement travaillé.
                                    </p>
                                </div>
                            </div>
                        </label>

                        <!-- Forfaitaire -->
                        <label class="group relative flex cursor-pointer rounded-xl border p-4 transition" :class="form.costType === 'FIXED'
                            ? 'border-blue-500 bg-blue-50/50 ring-1 ring-blue-500'
                            : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50'">
                            <input v-model="form.costType" type="radio" value="FIXED" :disabled="disabled"
                                class="sr-only" />

                            <div class="flex w-full items-start gap-3">
                                <div class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center
                                           rounded-full border transition" :class="form.costType === 'FIXED'
                                            ? 'border-blue-500 bg-blue-500'
                                            : 'border-slate-300 group-hover:border-slate-400'">
                                    <div v-if="form.costType === 'FIXED'" class="h-2 w-2 rounded-full bg-white" />
                                </div>

                                <div>
                                    <p class="text-sm font-semibold text-slate-800">
                                        Forfaitaire
                                    </p>

                                    <p class="mt-1 text-xs leading-relaxed text-slate-500">
                                        Basé sur le salaire forfaitaire du conducteur.
                                    </p>
                                </div>
                            </div>
                        </label>
                    </div>
                </div>


                <!-- TYPE DE CHAUFFEUR -->
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Type de chauffeur
                    </label>

                    <select v-model="form.driverType" required :disabled="disabled" class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm
                               outline-none transition
                               focus:border-blue-500 focus:ring-2 focus:ring-blue-100
                               disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option :value="null" disabled>
                            Sélectionner
                        </option>

                        <option value="LONG_HAUL">
                            Grand routier
                        </option>

                        <option value="SHORT_DISTANCE">
                            Grand régional
                        </option>

                        <option value="REGIONAL">
                            Régional
                        </option>
                    </select>
                </div>
            </div>
        </section>
    </div>
</template>