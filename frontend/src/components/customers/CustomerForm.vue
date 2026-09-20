<script setup lang="ts">
import type { CustomerFormData } from "@/models/Customer"

const form = defineModel<CustomerFormData>({
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
                    Informations générales du donneur d'ordre.
                </p>
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Nom
                    </label>
                    <input v-model="form.name" type="text" required maxlength="255" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>

                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Code
                    </label>
                    <input v-model="form.code" type="text" maxlength="100" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>
            </div>
        </section>

        <section class="space-y-4 border-t border-slate-200 pt-6">
            <div>
                <h3 class="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Adresse
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                    Coordonnées du site principal.
                </p>
            </div>

            <div>
                <label class="mb-1.5 block text-sm font-medium text-slate-700">
                    Adresse
                </label>
                <input v-model="form.address" type="text" maxlength="255" :disabled="disabled"
                    class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Ville
                    </label>
                    <input v-model="form.city" type="text" maxlength="100" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>

                <div>
                    <label class="mb-1.5 block text-sm font-medium text-slate-700">
                        Pays
                    </label>
                    <input v-model="form.country" type="text" maxlength="100" :disabled="disabled"
                        class="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-100" />
                </div>
            </div>
        </section>
    </div>
</template>