<script setup lang="ts">
import { ref, watch } from "vue"

import AppModal from "@/components/ui/AppModal.vue"
import CustomerForm from "@/components/customers/CustomerForm.vue"

import { updateCustomer } from "@/api/customerApi"
import { getApiErrorMessage } from "@/api/utils"

import { useNotification } from "@/composables/useNotification"

import type {
    CustomerDetails,
    CustomerFormData,
    CustomerUpdateRequest
} from "@/models/Customer"

const notification = useNotification()

const props = defineProps<{
    show: boolean
    customer: CustomerDetails | null
}>()

const emit = defineEmits<{
    close: []
    updated: []
}>()

const form = ref<CustomerFormData>({
    name: "",
    code: null,
    address: null,
    city: null,
    country: null,
})

const loading = ref(false)

watch(
    () => props.customer,
    (customer) => {
        if (!customer) return

        form.value = {
            name: customer.name,
            code: customer.code,
            address: customer.address,
            city: customer.city,
            country: customer.country,
        }
    },
    {
        immediate: true
    }
)

const closeModal = () => {
    if (!loading.value) {
        emit('close')
    }
}

const saveCustomer = async () => {
    if (!props.customer) return

    const request: CustomerUpdateRequest = {
        externalId: props.customer.externalId,
        externalSource: props.customer.externalSource,
        name: form.value.name.trim(),
        code: form.value.code?.trim() || null,
        address: form.value.address?.trim() || null,
        city: form.value.city?.trim() || null,
        country: form.value.country?.trim() || null
    }

    try {
        loading.value = true

        await updateCustomer(props.customer.id, request)

        notification.success(
            'Donneur d’ordre modifié',
            `Le donneur d’ordre « ${form.value.name.trim()} » a bien été modifié.`
        )

        emit('updated')
        emit('close')
    } catch (error) {
        notification.error(
            'Modification impossible',
            getApiErrorMessage(error, 'Le donneur d’ordre n’a pas pu être modifié.')
        )
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <AppModal :show="show" @close="closeModal">
        <h2 class="mb-6 text-2xl font-bold">
            Modifier le donneur d’ordre
        </h2>

        <CustomerForm v-model="form" :disabled="loading" />

        <div class="mt-6 flex justify-end gap-3">
            <button type="button" :disabled="loading" class="rounded-xl border px-4 py-2 disabled:opacity-50"
                @click="closeModal">
                Annuler
            </button>

            <button type="button" :disabled="loading || !form.name.trim()"
                class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:opacity-50"
                @click="saveCustomer">
                {{ loading ? 'Enregistrement' : 'Enregistrer' }}
            </button>
        </div>
    </AppModal>
</template>