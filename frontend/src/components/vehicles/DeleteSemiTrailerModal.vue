<script setup lang="ts">
import { ref } from 'vue'

import { deleteSemiTrailer } from '@/api/vehicle/semiTrailerApi'
import { getApiErrorMessage } from '@/api/utils'

import type { SemiTrailerSummary } from '@/models/vehicle/SemiTrailer'

import ConfirmDeleteModal from '@/components/ui/ConfirmDeleteModal.vue'
import { useNotification } from '@/composables/useNotification'
import { formatVehicleRegistration } from '@/utils/vehicleUtils'

const notification = useNotification()

const props = defineProps<{
    show: boolean
    semiTrailer: SemiTrailerSummary | null
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
    if (!props.semiTrailer) return

    try {
        deleting.value = true

        await deleteSemiTrailer(props.semiTrailer.id)

        notification.success(
            'Semi-remorque supprimée',
            `La semi-remorque « ${formatVehicleRegistration(props.semiTrailer.registration)} » a bien été supprimée.`,
        )

        emit('deleted')
        emit('close')
    } catch (error: unknown) {
        notification.error(
            'Suppression impossible',
            getApiErrorMessage(
                error,
                'La semi-remorque n’a pas pu être supprimée.',
            ),
        )
    } finally {
        deleting.value = false
    }
}
</script>

<template>
    <ConfirmDeleteModal :show="props.show" :loading="deleting" title="Supprimer la semi-remorque"
        :message="`Voulez-vous vraiment supprimer la semi-remorque ${props.semiTrailer ? formatVehicleRegistration(props.semiTrailer.registration) : ''} ?`"
        @close="closeModal" @confirm="confirmDelete" />
</template>