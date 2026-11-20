<script setup lang="ts">
import { ref } from 'vue'

import { deleteTractor } from '@/api/vehicle/tractorApi'
import { getApiErrorMessage } from '@/api/utils'

import type { TractorSummary } from '@/models/vehicle/Tractor'

import ConfirmDeleteModal from '@/components/ui/ConfirmDeleteModal.vue'
import { useNotification } from '@/composables/useNotification'
import { formatVehicleRegistration } from '@/utils/vehicleUtils'

const notification = useNotification()

const props = defineProps<{
    show: boolean
    tractor: TractorSummary | null
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
    if (!props.tractor) return

    try {
        deleting.value = true

        await deleteTractor(props.tractor.id)

        notification.success(
            'Tracteur supprimé',
            `Le tracteur « ${formatVehicleRegistration(props.tractor.registration)} » a bien été supprimé.`,
        )

        emit('deleted')
        emit('close')
    } catch (error: unknown) {
        notification.error(
            'Suppression impossible',
            getApiErrorMessage(error, 'Le tracteur n’a pas pu être supprimé.'),
        )
    } finally {
        deleting.value = false
    }
}
</script>

<template>
    <ConfirmDeleteModal :show="props.show" :loading="deleting" title="Supprimer le tracteur"
        :message="`Voulez-vous vraiment supprimer le tracteur ${props.tractor ? formatVehicleRegistration(props.tractor.registration) : ''} ?`"
        @close="closeModal" @confirm="confirmDelete" />
</template>