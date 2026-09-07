<script setup lang="ts">
import { reactive, ref, watch } from "vue"


import { getApiErrorMessage } from '@/api/utils'
import { createEmptyDriverForm } from "@/utils/driverUtils"
import type { CreateDriverRequest, DriverFormData } from "@/models/driver/Driver"

import AppModal from '@/components/ui/AppModal.vue'
import { useNotification } from '@/composables/useNotification'
import { createDriver } from "@/api/driver/driverApi"
import DriverForm from "./DriverForm.vue"

//Variables

const notification = useNotification()
const loading = ref(false)

const props = defineProps<{
    show: boolean
}>()

const emit = defineEmits<{
    close: []
    created: []
}>()

//Manage Modal

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

// Manage form

const form = reactive<DriverFormData>(
    createEmptyDriverForm(),
)

const resetForm = () => {
    Object.assign(form, createEmptyDriverForm())
}

const submitDriver = async () => {
    try {
        loading.value = true

        const payload: CreateDriverRequest = {
            firstName: form.firstName,
            lastName: form.lastName,

            phoneNumber: form.phoneNumber,
            annualSalary: form.annualSalary,
            monthlyWorkingHours: form.monthlyWorkingHours,
            tractorId: form.tractorId,
            semiTrailerId: form.semiTrailerId,
            costType: form.costType,
            driverType: form.driverType,
            locationLabel: form.locationLabel,
            locationLatitude: form.locationLatitude,
            locationLongitude: form.locationLongitude,
        }

        await createDriver(payload)

        notification.success(
            "Conducteur créé",
            `Le conducteur « ${form.firstName} ${form.lastName} » a bien été ajouté.`,
        )

        emit("created")
    } catch (error: unknown) {
        notification.error(
            "Création impossible",
            getApiErrorMessage(error, "Le conducteur n’a pas pu être créé."),
        )
    } finally {
        loading.value = false
    }
}

//Watch

watch(
    () => props.show,
    (show) => {
        if (show) {
            resetForm()
        }
    },
)
</script>

<template>

    <AppModal :show="props.show" @close="emit('close')">

        <form @submit.prevent="submitDriver">
            <h2 class="mb-6 text-2xl font-bold text-slate-900">
                Ajouter un conducteur
            </h2>

            <div class="max-h-[70vh] overflow-y-auto pr-2">
                <DriverForm v-model="form" :disabled="loading" />
            </div>

            <div class="mt-6 flex justify-end gap-3">
                <button type="button" :disabled="loading"
                    class="rounded-xl border border-slate-300 px-4 py-2 text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                    @click="closeModal">
                    Annuler
                </button>

                <button type="submit" :disabled="loading"
                    class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                    {{ loading ? "Enregistrement" : "Enregistrer" }}
                </button>
            </div>
        </form>

    </AppModal>

</template>