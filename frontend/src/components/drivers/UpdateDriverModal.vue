<script setup lang="ts">
import { reactive, ref, watch } from "vue"

import { updateDriver } from "@/api/driver/driverApi"
import { getApiErrorMessage } from "@/api/utils"

import type { DriverDetails, DriverFormData, UpdateDriverRequest } from "@/models/driver/Driver"

import { createEmptyDriverForm } from "@/utils/driverUtils"

import AppModal from "@/components/ui/AppModal.vue"
import DriverForm from "@/components/drivers/DriverForm.vue"

import { useNotification } from "@/composables/useNotification"

//Variables

const notification = useNotification()
const loading = ref(false)

const props = defineProps<{
    show: boolean
    driver: DriverDetails | null
}>()

const emit = defineEmits<{
    close: []
    updated: []
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

const populateForm = () => {
    if (!props.driver) {
        return
    }

    Object.assign(form, {
        firstName: props.driver.firstName,
        lastName: props.driver.lastName,

        phoneNumber: props.driver.phoneNumber,
        annualSalary: props.driver.annualSalary,
        monthlyWorkingHours: props.driver.monthlyWorkingHours,
        tractorId: props.driver.tractorId,
        semiTrailerId: props.driver.semiTrailerId,
        costType: props.driver.costType,
        driverType: props.driver.driverType,
        locationLabel: props.driver.locationLabel,
        locationLatitude: props.driver.locationLatitude,
        locationLongitude: props.driver.locationLongitude,
    } satisfies DriverFormData)
}

const submitDriver = async () => {
    if (!props.driver) {
        return
    }

    try {
        loading.value = true

        const payload: UpdateDriverRequest = {
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

        await updateDriver(props.driver.id, payload)

        notification.success(
            "Conducteur modifié",
            `Le conducteur « ${form.firstName} ${form.lastName} » a bien été modifié.`,
        )

        emit("updated")
    } catch (error: unknown) {
        notification.error(
            "Modification impossible",
            getApiErrorMessage(error, "Le conducteur n’a pas pu être modifié."),
        )
    } finally {
        loading.value = false
    }
}

//Watch

watch(
    [() => props.show, () => props.driver],
    ([show, driver]) => {
        if (show && driver) {
            populateForm()
        }
    },
    {
        immediate: true,
    },
)
</script>

<template>
    <AppModal :show="props.show" @close="closeModal">
        <form @submit.prevent="submitDriver">
            <h2 class="mb-6 text-2xl font-bold text-slate-900">
                Modifier un conducteur
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