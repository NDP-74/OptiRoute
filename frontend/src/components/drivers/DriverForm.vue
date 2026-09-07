<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { getTractors } from "@/api/vehicle/tractorApi"
import { getSemiTrailers } from "@/api/vehicle/semiTrailerApi"
import type { TractorSummary } from "@/models/vehicle/Tractor"
import type { SemiTrailerSummary } from "@/models/vehicle/SemiTrailer"
import type { DriverFormData } from "@/models/driver/Driver"
import { formatVehicleLabel } from "@/utils/vehicleUtils"
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

<template>
    <div class="space-y-6">
        <section class="space-y-4">
            <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                Identification
            </h3>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Prénom
                    </label>

                    <input v-model="form.firstName" type="text" required maxlength="100" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Nom
                    </label>

                    <input v-model="form.lastName" type="text" required maxlength="100" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>
            </div>

            <div>
                <label class="mb-1 block text-sm font-medium text-slate-700">
                    Numéro de téléphone
                </label>

                <input v-model="form.phoneNumber" type="tel" maxlength="30" :disabled="disabled"
                    class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />
            </div>

            <HereAutocompleteInput v-model="parkingLocation" label="Lieu de stationnement" :disabled="disabled" />

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">Tracteur</label>
                    <select v-model="form.tractorId" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option :value="null">Aucun tracteur</option>
                        <option v-for="tractor in tractors" :key="tractor.id" :value="tractor.id">
                            {{ formatVehicleLabel(tractor.brand, tractor.model) }} - {{ tractor.registration }}
                        </option>
                    </select>
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">Semi-remorque</label>
                    <select v-model="form.semiTrailerId" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option :value="null">Aucune semi-remorque</option>
                        <option v-for="semiTrailer in semiTrailers" :key="semiTrailer.id" :value="semiTrailer.id">
                            {{ formatVehicleLabel(semiTrailer.brand, semiTrailer.model) }} - {{ semiTrailer.registration
                            }}
                        </option>
                    </select>
                </div>
            </div>

            <p v-if="optionsError" class="text-sm text-red-600">Impossible de charger les véhicules.</p>
        </section>

        <section class="space-y-4">
            <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                Coût et temps de travail
            </h3>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Coût annuel
                    </label>

                    <input v-model.number="form.annualSalary" type="number" min="0" step="100" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Euros par an
                    </p>

                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">
                        Temps de travail mensuel
                    </label>

                    <input v-model.number="form.monthlyWorkingHours" type="number" min="0" step="1" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100" />

                    <p class="mt-1 text-xs text-slate-500">
                        Heures par mois
                    </p>
                </div>
            </div>
        </section>

        <section class="space-y-4">
            <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">Profil du chauffeur</h3>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">Type de coût</label>
                    <select v-model="form.costType" required :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option :value="null" disabled>Sélectionner</option>
                        <option value="HOURLY">Horaire</option>
                        <option value="FIXED">Fixe</option>
                    </select>
                </div>

                <div>
                    <label class="mb-1 block text-sm font-medium text-slate-700">Type de chauffeur</label>
                    <select v-model="form.driverType" required :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition focus:border-blue-500 disabled:cursor-not-allowed disabled:bg-slate-100">
                        <option :value="null" disabled>Sélectionner</option>
                        <option value="LONG_HAUL">Grand routier</option>
                        <option value="SHORT_DISTANCE">Courte distance</option>
                        <option value="REGIONAL">Régional</option>
                    </select>
                </div>
            </div>
        </section>
    </div>
</template>