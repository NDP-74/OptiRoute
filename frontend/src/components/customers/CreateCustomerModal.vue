```vue
<script setup lang="ts">
import { ref } from "vue"

import AppModal from "@/components/ui/AppModal.vue"
import CustomerForm from "@/components/customers/CustomerForm.vue"

import { createCustomer } from "@/api/customerApi"
import { getApiErrorMessage } from "@/api/utils"

import type { CustomerCreateRequest, CustomerFormData } from "@/models/Customer"
import { useNotification } from "@/composables/useNotification"

const notification = useNotification()

const props = defineProps<{
    show: boolean
}>()

const emit = defineEmits<{
    close: []
    created: []
}>()

const initialForm = (): CustomerFormData => ({
    name: "",
    code: null,
    address: null,
    city: null,
    country: "France",
})

const form = ref<CustomerFormData>(initialForm())

const loading = ref(false)

const submitCustomer = async () => {

    try {
        loading.value = true

        const payload: CustomerCreateRequest = {
            externalId: null,
            externalSource: "MANUAL",
            name: form.value.name.trim(),
            code: form.value.code?.trim() || null,
            address: form.value.address?.trim() || null,
            city: form.value.city?.trim() || null,
            country: form.value.country?.trim() || null
        }

        await createCustomer(payload)

        notification.success(
            'Donneur d’ordre créé',
            `Le donneur d’ordre « ${form.value.name.trim()} » a bien été créé.`
        )

        emit('created')
        emit('close')

        resetForm()
    } catch (e) {
        notification.error('Création impossible', getApiErrorMessage(e, 'Donneur d’ordre n’a pas pu être créé.'))
    } finally {
        loading.value = false
    }
}

const resetForm = () => {
    form.value = initialForm()
}
</script>

<template>

    <AppModal :show="props.show" @close="emit('close')">

        <h2 class="text-2xl font-bold mb-6">
            Ajouter un nouveau donneur d’ordre
        </h2>

        <CustomerForm v-model="form" :disabled="loading" />

        <div class="flex justify-end gap-3 mt-6">

            <button type="button" :disabled="loading" class="rounded-xl border px-4 py-2 disabled:opacity-50"
                @click="emit('close')">
                Annuler
            </button>

            <button type="button" :disabled="loading || !form.name.trim()" @click="submitCustomer"
                class="rounded-xl bg-blue-600 px-4 py-2 text-white transition hover:bg-blue-700 disabled:opacity-50">
                {{ loading ? 'Enregistrement' : 'Enregistrer' }}
            </button>

        </div>

    </AppModal>

</template>
```
