<script setup lang="ts">
import { ref } from 'vue'

import { deleteCustomer } from '@/api/customerApi'
import { getApiErrorMessage } from '@/api/utils'

import type { CustomerSummary } from '@/models/Customer'

import ConfirmDeleteModal from '@/components/ui/ConfirmDeleteModal.vue'
import { useNotification } from '@/composables/useNotification'

const notification = useNotification()

const props = defineProps<{
    show: boolean
    customer: CustomerSummary | null
}>()

const emit = defineEmits<{
    close: []
    deleted: []
}>()

const deleting = ref(false)

const closeModal = () => {
    if (!deleting.value) {
        emit('close')
    }
}

const confirmDelete = async () => {
    if (!props.customer) return

    try {
        deleting.value = true

        await deleteCustomer(props.customer.id)

        notification.success(
            'Donneur d’ordre supprimé',
            `Le donneur d’ordre « ${props.customer?.name ?? ''} » a bien été supprimé.`
        )

        emit('deleted')
        emit('close')
    } catch (error) {
        notification.error('Suppression impossible', getApiErrorMessage(error, 'Le donneur d’ordre n’a pas pu être supprimé.')
        )
    } finally {
        deleting.value = false
    }
}
</script>

<template>
    <ConfirmDeleteModal :show="show" :loading="deleting" title="Supprimer le donneur d’ordre"
        :message="`Voulez-vous vraiment supprimer le donneur d’ordre ${customer?.name ?? ''} ?`" @close="closeModal"
        @confirm="confirmDelete" />
</template>