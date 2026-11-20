<script setup lang="ts">
import { reactive, ref, watch } from "vue"

import { createDriver, updateDriver } from "@/api/driver/driverApi"
import { getApiErrorMessage } from "@/api/utils"

import type { CreateDriverRequest, DriverDetails, DriverFormData } from "@/models/driver/Driver"
import { createEmptyDriverForm } from "@/utils/driverUtils"

import AppModal from "@/components/ui/AppModal.vue"
import DriverForm from "@/components/drivers/DriverForm.vue"

import { useNotification } from "@/composables/useNotification"

const notification = useNotification()
const loading = ref(false)

const props = defineProps<{
    show: boolean
    driver?: DriverDetails | null
}>()

const emit = defineEmits<{
    close: []
    created: [driver: DriverDetails]
    updated: []
}>()

const form = reactive<DriverFormData>(createEmptyDriverForm())

const syncForm = () => {
    if (props.driver) {
        Object.assign(form, {
            firstName: props.driver.firstName,
            lastName: props.driver.lastName,
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
    } else {
        Object.assign(form, createEmptyDriverForm())
    }
}

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

const submitDriver = async () => {
    try {
        loading.value = true

        const payload: CreateDriverRequest = {
            firstName: form.firstName,
            lastName: form.lastName,
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

        if (props.driver) {
            await updateDriver(props.driver.id, payload)

            notification.success(
                "Conducteur modifié",
                `Le conducteur « ${form.firstName} ${form.lastName} » a bien été modifié.`,
            )
            emit("updated")
        } else {
            const driver = await createDriver(payload)

            notification.success(
                "Conducteur créé",
                `Le conducteur « ${form.firstName} ${form.lastName} » a bien été ajouté.`,
            )
            emit("created", driver)
        }
    } catch (error: unknown) {
        const action = props.driver ? "Modification" : "Création"
        const fallback = props.driver
            ? "Le conducteur n’a pas pu être modifié."
            : "Le conducteur n’a pas pu être créé."

        notification.error(`${action} impossible`, getApiErrorMessage(error, fallback))
    } finally {
        loading.value = false
    }
}

watch(
    [() => props.show, () => props.driver],
    ([show]) => {
        if (show) {
            syncForm()
        } else {
            Object.assign(form, createEmptyDriverForm())
        }
    },
    { immediate: true },
)
</script>

<template>
    <AppModal :show="props.show" @close="closeModal">
        <form @submit.prevent="submitDriver">
            <h2 class="mb-6 text-2xl font-bold text-slate-900">
                {{ props.driver ? "Modifier un conducteur" : "Ajouter un conducteur" }}
            </h2>

            <div class="max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar">
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
