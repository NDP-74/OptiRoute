<script setup lang="ts">
import { ref, watch } from "vue"

import AppModal from "@/components/ui/AppModal.vue"
import CustomerForm from "@/components/customers/CustomerForm.vue"

import { createCustomer, updateCustomer } from "@/api/customerApi"
import { getApiErrorMessage } from "@/api/utils"

import type { CustomerCreateRequest, CustomerDetails, CustomerFormData } from "@/models/Customer"
import { useNotification } from "@/composables/useNotification"

const notification = useNotification()

const props = defineProps<{
    show: boolean
    customer?: CustomerDetails | null
}>()

const emit = defineEmits<{
    close: []
    created: [customer: CustomerDetails]
    updated: []
}>()

const initialForm = (): CustomerFormData => ({
    name: "",
    code: null,
    address: null,
})

const form = ref<CustomerFormData>(initialForm())
const loading = ref(false)

const syncForm = () => {
    form.value = props.customer
        ? {
            name: props.customer.name,
            code: props.customer.code,
            address: props.customer.address,
        }
        : initialForm()
}

const closeModal = () => {
    if (!loading.value) {
        emit("close")
    }
}

const submitCustomer = async () => {
    try {
        loading.value = true

        const payload: CustomerCreateRequest = {
            name: form.value.name.trim(),
            code: form.value.code?.trim() || null,
            address: form.value.address?.trim() || null,
        }

        if (props.customer) {
            await updateCustomer(props.customer.id, payload)
            notification.success(
                "Donneur d’ordre modifié",
                `Le donneur d’ordre « ${payload.name} » a bien été modifié.`,
            )
            emit("updated")
            emit("close")
        } else {
            const customer = await createCustomer(payload)
            notification.success(
                "Donneur d’ordre créé",
                `Le donneur d’ordre « ${payload.name} » a bien été créé.`,
            )
            emit("created", customer)
            emit("close")
        }
    } catch (error: unknown) {
        const action = props.customer ? "Modification" : "Création"
        const fallback = props.customer
            ? "Le donneur d’ordre n’a pas pu être modifié."
            : "Le donneur d’ordre n’a pas pu être créé."

        notification.error(`${action} impossible`, getApiErrorMessage(error, fallback))
    } finally {
        loading.value = false
    }
}

watch(
    [() => props.show, () => props.customer],
    ([show]) => {
        if (show) {
            syncForm()
        } else {
            form.value = initialForm()
        }
    },
    { immediate: true },
)
</script>

<template>
    <AppModal :show="props.show" @close="closeModal">
        <form @submit.prevent="submitCustomer">
            <h2 class="mb-6 text-2xl font-bold">
                {{ props.customer ? "Modifier le donneur d’ordre" : "Ajouter un nouveau donneur d’ordre" }}
            </h2>

            <CustomerForm v-model="form" :disabled="loading" />

            <div class="mt-6 flex justify-end gap-3">
                <button type="button" :disabled="loading" class="rounded-xl border px-4 py-2 disabled:opacity-50"
                    @click="closeModal">
                    Annuler
                </button>

                <button type="submit" :disabled="loading || !form.name.trim()"
                    class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:opacity-50">
                    {{ loading ? "Enregistrement" : "Enregistrer" }}
                </button>
            </div>
        </form>
    </AppModal>
</template>
